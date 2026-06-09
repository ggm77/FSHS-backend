package com.seohamin.fshs.v2.global.infra.ffmpeg;

import com.seohamin.fshs.v2.global.config.FfmpegConfig;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.ffmpeg.dto.FfmpegAnalysisResultDto;
import com.seohamin.fshs.v2.global.infra.json.JsonMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class FfmpegProcessor {

    private final FfmpegConfig ffmpegConfig;
    private final JsonMapper jsonMapper;

    private final Map<HlsCacheKey, HlsSession> hlsSessions = new ConcurrentHashMap<>();

    private static final int HLS_SEGMENT_SECONDS = 5;
    private static final long HLS_OUTPUT_WAIT_TIMEOUT_MILLIS = 60_000L;
    private static final Pattern HLS_FILE_PATTERN = Pattern.compile("index\\.m3u8|segment\\d{1,9}\\.ts");
    private static final Path HLS_WORK_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "fshs-hls"
    ).toAbsolutePath().normalize();

    /**
     * FFprobe를 통해 파일을 분석하는 메서드
     * @param filePath 분석할 파일의 정규화 된 경로
     * @return 분석 결과가 담긴 DTO
     */
    public FfmpegAnalysisResultDto analyze(final Path filePath) {
        final List<String> command = List.of(
                ffmpegConfig.getFfprobe(),
                "-v", "error",
                "-show_format",
                "-show_streams",
                "-of", "json",
                filePath.toString()
        );

        return jsonMapper.fromJson(
                execute(command, 30),
                FfmpegAnalysisResultDto.class
        );
    }

    /**
     * 저장된 파일을 실시간 트랜스코딩하는 메서드
     * h264 mp4 / aac로 변환
     * @param filePath 파일 절대 경로
     * @param start 영상 시작 시점
     * @return 트랜스코딩된 영상 스트림 Response
     */
    public StreamingResponseBody getVideoStream(
            final String filePath,
            final double start
    ) {
        final String encoder = ffmpegConfig.getSelectedH264Encoder();
        final List<String> encoderOpts = getEncoderOptions(encoder);
        final boolean zeroCopy = useCudaZeroCopy(Path.of(filePath));

        final List<String> command = new java.util.ArrayList<>();
        command.add(ffmpegConfig.getFfmpeg());
        command.add("-loglevel");
        command.add("error");
        command.addAll(getHwAccelOptions(zeroCopy));
        command.add("-ss");
        command.add(String.valueOf(start));
        command.add("-i");
        command.add(filePath);
        command.addAll(encoderOpts);
        if (!zeroCopy) {
            command.add("-pix_fmt");
            command.add("yuv420p");
        }
        command.addAll(List.of(
                "-acodec", "aac",
                "-b:a", "128k",
                "-f", "mp4",
                "-movflags", "frag_keyframe+empty_moov+default_base_moof+omit_tfhd_offset",
                "-flush_packets", "1",
                "pipe:1"
        ));

        return outputStream -> {
            final ProcessBuilder pb = new ProcessBuilder(command);
            final Process process = pb.start();

            // 에러 스트림(stderr)을 비동기적으로 읽어 에러 발생 시 로그에 기록
            final Thread errorReader = Thread.ofPlatform().start(() -> {
                try (final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error("[FFmpeg 프로세스 에러] {}", line);
                    }
                } catch (final Exception ignored) {}
            });

            try (final InputStream is = process.getInputStream()) {
                is.transferTo(outputStream);
            } catch (final Exception ex) {
                if (!isClientAbort(ex)) {
                    log.error("[FFmpeg 에러 발생] {}", ex.getMessage());
                    throw new CustomException(ExceptionCode.FFMPEG_ERROR, ex);
                }
                else {
                    log.info(ex.getMessage());
                }
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                try {
                    errorReader.join(1000);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    /**
     * FFmpeg HLS muxer 한 프로세스로 EVENT playlist와 연속된 세그먼트를 생성하고,
     * 생성된 산출물(index.m3u8 또는 segmentN.ts)을 파일 Resource로 반환한다.
     * @param filePath 원본 파일 절대 경로
     * @param hlsFile 요청한 HLS 파일명
     * @return HLS 산출물 Resource
     */
    public Resource getHlsFile(
            final Path filePath,
            final String hlsFile
    ) {
        if (filePath == null || hlsFile == null || !HLS_FILE_PATTERN.matcher(hlsFile).matches()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final HlsSession session = ensureHlsSession(filePath);
        final Path output = session.outputDir().resolve(hlsFile).normalize();
        if (!output.startsWith(session.outputDir())) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        waitForHlsOutput(output, session);
        return new FileSystemResource(output);
    }

    /**
     * 원본 파일에 대한 HLS 변환 세션을 확보하는 메서드
     * 같은 파일(경로·크기·수정시각 동일)에 대해서는 단일 FFmpeg 프로세스를 공유해
     * 연속된 타임스탬프의 세그먼트를 만들고, 이미 완료된 세션은 디스크 산출물을 재사용한다
     * @param filePath 원본 파일 절대 경로
     * @return 확보된 HLS 세션
     */
    private HlsSession ensureHlsSession(final Path filePath) {
        final HlsCacheKey key = buildHlsCacheKey(filePath);
        evictOtherVersions(key);
        return hlsSessions.compute(key, (k, existing) -> {
            if (existing != null && existing.isReusable()) {
                return existing;
            }
            if (existing != null) {
                existing.destroy();
            }
            return startHlsSession(k);
        });
    }

    /**
     * 파일의 경로·크기·수정시각으로 세션 캐시 키를 만드는 메서드
     * 파일이 교체되면 크기/수정시각이 달라져 새 세션으로 분리된다
     * @param filePath 원본 파일 절대 경로
     * @return 세션 캐시 키
     */
    private HlsCacheKey buildHlsCacheKey(final Path filePath) {
        try {
            final BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            return new HlsCacheKey(
                    filePath.toAbsolutePath().normalize().toString(),
                    attrs.size(),
                    attrs.lastModifiedTime().toMillis()
            );
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST, ex);
        }
    }

    /**
     * 같은 경로의 옛 버전 세션(크기/수정시각이 달라진 키)을 정리하는 메서드
     * 파일이 in-place 로 교체됐을 때 옛 프로세스·작업 디렉터리가 쌓이지 않게 한다
     * @param keep 유지할 현재 키
     */
    private void evictOtherVersions(final HlsCacheKey keep) {
        hlsSessions.keySet().removeIf(k -> {
            if (k.path().equals(keep.path()) && !k.equals(keep)) {
                final HlsSession stale = hlsSessions.get(k);
                if (stale != null) {
                    stale.destroy();
                }
                return true;
            }
            return false;
        });
    }

    /**
     * FFmpeg HLS muxer 한 프로세스를 띄워 새 변환 세션을 시작하는 메서드
     * 작업 디렉터리를 깨끗이 비운 뒤 index.m3u8 / segmentN.ts 를 그 디렉터리에 출력한다
     * @param key 세션 캐시 키
     * @return 시작된 HLS 세션
     */
    private HlsSession startHlsSession(final HlsCacheKey key) {
        final Path outputDir = HLS_WORK_ROOT.resolve(hashKey(key));
        try {
            deleteRecursively(outputDir);
            Files.createDirectories(outputDir);
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FFMPEG_ERROR, ex);
        }

        final List<String> command = buildHlsCommand(Path.of(key.path()), outputDir);
        try {
            final ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            final Process process = pb.start();

            // stderr 를 비동기로 소비 — 버퍼가 차서 프로세스가 멈추는 것을 막고 에러를 로깅한다
            final Thread errorReader = Thread.ofPlatform().start(() -> {
                try (final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error("[FFmpeg HLS 프로세스 에러] {}", line);
                    }
                } catch (final Exception ignored) {}
            });

            return new HlsSession(outputDir, process, errorReader);
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FFMPEG_ERROR, ex);
        }
    }

    /**
     * 단일 프로세스로 EVENT 재생목록과 연속 세그먼트를 만드는 FFmpeg HLS 명령을 조립하는 메서드
     * 세그먼트 경계마다 keyframe 을 강제(-force_key_frames)해 hls_time 과 정렬하고,
     * Safari 호환을 위해 H.264 / AAC / yuv420p 로 출력한다
     * @param input 원본 파일 절대 경로
     * @param outputDir 산출물을 쓸 작업 디렉터리
     * @return FFmpeg 명령 리스트
     */
    private List<String> buildHlsCommand(
            final Path input,
            final Path outputDir
    ) {
        final List<String> encoderOpts = getEncoderOptions(ffmpegConfig.getSelectedH264Encoder());
        final boolean zeroCopy = useCudaZeroCopy(input);

        final List<String> command = new java.util.ArrayList<>();
        command.add(ffmpegConfig.getFfmpeg());
        command.add("-loglevel");
        command.add("error");
        command.add("-y");
        command.addAll(getHwAccelOptions(zeroCopy));
        command.add("-i");
        command.add(input.toString());
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("0:a:0?");
        command.addAll(encoderOpts);
        // 세그먼트 시작점이 항상 keyframe 이 되도록 hls_time 주기로 keyframe 강제 (Safari 민감)
        command.add("-force_key_frames");
        command.add("expr:gte(t,n_forced*" + HLS_SEGMENT_SECONDS + ")");
        if (!zeroCopy) {
            command.add("-pix_fmt");
            command.add("yuv420p");
        }
        command.addAll(List.of(
                "-acodec", "aac",
                "-b:a", "128k",
                "-f", "hls",
                "-hls_time", String.valueOf(HLS_SEGMENT_SECONDS),
                // EVENT: 변환 중에는 ENDLIST 없이 자라고, 완료 시 FFmpeg 가 ENDLIST 를 붙인다
                "-hls_playlist_type", "event",
                // temp_file: 세그먼트/재생목록을 .tmp 로 쓰고 원자적 rename → 미완성 파일 노출 방지
                "-hls_flags", "independent_segments+temp_file",
                "-hls_segment_filename", outputDir.resolve("segment%d.ts").toString(),
                outputDir.resolve("index.m3u8").toString()
        ));
        return command;
    }

    /**
     * 요청한 HLS 산출물이 디스크에 나타날 때까지 대기하는 메서드
     * 프로세스가 끝났는데도 파일이 없으면(실패 또는 범위 밖 세그먼트) 예외를 던진다
     * @param output 기다릴 산출물 경로
     * @param session 해당 HLS 세션
     */
    private void waitForHlsOutput(
            final Path output,
            final HlsSession session
    ) {
        final long deadline = System.currentTimeMillis() + HLS_OUTPUT_WAIT_TIMEOUT_MILLIS;
        while (true) {
            if (Files.exists(output)) {
                return;
            }
            if (!session.process().isAlive()) {
                // 프로세스 종료 후에도 없으면 변환 실패이거나 존재하지 않는 세그먼트
                throw new CustomException(ExceptionCode.FFMPEG_ERROR);
            }
            if (System.currentTimeMillis() > deadline) {
                throw new CustomException(ExceptionCode.COMMAND_TIMEOUT);
            }
            try {
                Thread.sleep(80L);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CustomException(ExceptionCode.PROCESS_INTERRUPTED);
            }
        }
    }

    /**
     * 캐시 키를 작업 디렉터리 이름으로 쓸 수 있게 SHA-256 16진 문자열로 만드는 메서드
     * @param key 세션 캐시 키
     * @return 16진 해시 문자열
     */
    private static String hashKey(final HlsCacheKey key) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(
                    (key.path() + "|" + key.size() + "|" + key.lastModified())
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (final NoSuchAlgorithmException ex) {
            throw new CustomException(ExceptionCode.FFMPEG_ERROR, ex);
        }
    }

    /**
     * 디렉터리 트리를 하위부터 역순으로 삭제하는 메서드
     * @param dir 삭제할 디렉터리
     */
    private static void deleteRecursively(final Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (final java.util.stream.Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (final IOException ignored) {}
            });
        } catch (final IOException ignored) {}
    }

    /**
     * 애플리케이션 종료 시 모든 HLS 프로세스를 종료하고 작업 디렉터리를 정리하는 메서드
     */
    @PreDestroy
    void shutdownHlsSessions() {
        hlsSessions.values().forEach(HlsSession::destroy);
        hlsSessions.clear();
        deleteRecursively(HLS_WORK_ROOT);
    }

    /**
     * HLS 세션 캐시 키 — 같은 경로라도 크기/수정시각이 다르면 다른 세션으로 본다
     */
    private record HlsCacheKey(String path, long size, long lastModified) {}

    /**
     * 진행 중이거나 완료된 단일 FFmpeg HLS 변환 세션
     */
    private record HlsSession(Path outputDir, Process process, Thread errorReader) {

        /**
         * 재사용 가능한 세션인지 판별한다 — 진행 중이거나, 정상 종료(exit 0)했고 재생목록이 아직 남아있을 때
         */
        boolean isReusable() {
            if (process.isAlive()) {
                return true;
            }
            return process.exitValue() == 0 && Files.exists(outputDir.resolve("index.m3u8"));
        }

        /**
         * 프로세스를 강제 종료하고 작업 디렉터리를 정리한다
         */
        void destroy() {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            try {
                errorReader.join(500L);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            deleteRecursively(outputDir);
        }
    }

    private List<String> getEncoderOptions(final String encoder) {
        if (encoder == null || encoder.equals("libx264")) {
            return List.of(
                    "-vcodec", "libx264",
                    "-preset", "ultrafast",
                    "-tune", "zerolatency",
                    "-g", "30",
                    "-bf", "0"
            );
        }

        if (encoder.contains("nvenc")) {
            return List.of(
                    "-vcodec", encoder,
                    "-preset", "p1",
                    "-bf", "0",
                    "-delay", "0",
                    "-g", "30",
                    "-forced-idr", "1"
            );
        } else if (encoder.contains("videotoolbox")) {
            return List.of(
                    "-vcodec", encoder,
                    "-realtime", "true",
                    "-g", "30",
                    "-bf", "0"
            );
        } else if (encoder.contains("qsv")) {
            return List.of(
                    "-vcodec", encoder,
                    "-preset", "veryfast",
                    "-g", "30",
                    "-bf", "0"
            );
        } else if (encoder.contains("v4l2m2m")) {
            return List.of(
                    "-vcodec", encoder,
                    "-g", "30",
                    "-bf", "0"
            );
        } else {
            return List.of(
                    "-vcodec", encoder,
                    "-preset", "ultrafast",
                    "-tune", "zerolatency",
                    "-g", "30",
                    "-bf", "0"
            );
        }
    }

    /**
     * 설정된 하드웨어 가속 API로 입력 디코딩을 GPU에 위임하는 옵션을 생성하는 메서드
     * -hwaccel 은 입력(-i) 앞에 와야 하며, 미설정 시 소프트웨어 디코딩으로 동작한다
     * zeroCopy 인 경우 -hwaccel_output_format 까지 지정해 디코딩 프레임을 GPU 메모리에 유지한다
     * @param zeroCopy 디코딩 결과를 GPU 메모리에 둘지 여부 (VRAM↔RAM 왕복 제거)
     * @return -hwaccel 옵션 리스트 (미설정 시 빈 리스트)
     */
    private List<String> getHwAccelOptions(final boolean zeroCopy) {
        final String api = ffmpegConfig.getSelectedHwAccelApi();
        if (api == null || api.isBlank()) {
            return List.of();
        }
        if (zeroCopy) {
            return List.of("-hwaccel", api, "-hwaccel_output_format", api);
        }
        return List.of("-hwaccel", api);
    }

    /**
     * 완전 GPU(zero-copy) 트랜스코딩 경로를 쓸 수 있는지 판별하는 메서드
     * cuda + nvenc 조합이고, 소스 코덱을 NVDEC 가 디코딩할 수 있으며, 8비트 4:2:0 일 때만 가능하다
     * 그 외(NVDEC 미지원 코덱·10비트·4:2:2·4:4:4 등)는 GPU 측 변환 필터(scale_cuda)가 없는
     * 바닐라 빌드에서 깨지므로, -hwaccel_output_format 없이 소프트웨어 디코딩 + CPU 픽셀포맷 변환으로 처리한다
     * cuda + nvenc 가 아니면 소스 분석(ffprobe) 없이 바로 false 를 반환한다
     * @param filePath 파일 절대 경로
     * @return zero-copy 사용 가능 여부
     */
    private boolean useCudaZeroCopy(final Path filePath) {
        final String api = ffmpegConfig.getSelectedHwAccelApi();
        final String enc = ffmpegConfig.getSelectedH264Encoder();
        if (!"cuda".equalsIgnoreCase(api) || enc == null || !enc.contains("nvenc")) {
            return false;
        }

        final FfmpegAnalysisResultDto analysis;
        try {
            analysis = analyze(filePath);
        } catch (final Exception ex) {
            log.warn("[소스 분석 실패 — 소프트웨어 변환 경로로 폴백] {}", ex.getMessage());
            return false;
        }
        return isNvdecDecodable(analysis.getVideoCodec())
                && isEightBit420(analysis.getVideoPixFmt());
    }

    /**
     * NVDEC 가 디코딩 가능한 코덱인지 판별하는 메서드 (보수적 화이트리스트)
     * 목록에 없는 코덱(mjpeg, theora 등)은 zero-copy 에서 제외해 소프트웨어 디코딩으로 폴백시킨다
     * av1 은 구형 GPU(Ampere 미만)에서 NVDEC 디코딩이 안 되므로 제외한다
     * @param codec 소스 비디오 코덱명
     * @return NVDEC 디코딩 가능 여부
     */
    private boolean isNvdecDecodable(final String codec) {
        if (codec == null) {
            return false;
        }
        return switch (codec) {
            case "h264", "hevc", "vp9", "mpeg2video", "vc1" -> true;
            default -> false;
        };
    }

    /**
     * NVDEC 가 추가 변환 없이 NV12 로 디코딩 가능한 8비트 4:2:0 포맷인지 판별하는 메서드
     * @param pixFmt 소스 픽셀 포맷
     * @return 8비트 4:2:0 여부
     */
    private boolean isEightBit420(final String pixFmt) {
        if (pixFmt == null) {
            return false;
        }
        return switch (pixFmt) {
            case "yuv420p", "yuvj420p", "nv12", "nv21" -> true;
            default -> false;
        };
    }

    /**
     * FFmpeg 명령어를 실행하는 메서드
     * timeoutSecond로 타임아웃 시간을 설정할 수 있음.
     * @param command 실행할 명령어
     * @param timeoutSecond 타임아웃 시간 (초)
     * @return 실행 결과 문자열
     */
    private String execute(
            final List<String> command,
            final int timeoutSecond
    ) {
        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            final Process p = pb.start();

            // 별도 스레드에서 stdout 소비 — waitFor 전에 읽기 블로킹 방지
            final StringBuilder output = new StringBuilder();
            final Thread readerThread = Thread.ofPlatform().start(() -> {
                try (final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines().forEach(line -> output.append(line).append("\n"));
                } catch (final java.io.IOException ignored) {}
            });

            if (!p.waitFor(timeoutSecond, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.error("[FFmpeg 타임아웃] - 명령어: {}", command);
                throw new CustomException(ExceptionCode.COMMAND_TIMEOUT);
            }

            readerThread.join(1000);
            return output.toString();
        } catch (final CustomException ex) {
            throw ex;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CustomException(ExceptionCode.PROCESS_INTERRUPTED);
        } catch (final Exception ex) {
            log.error("[FFmpeg 에러 발생] {}", ex.getMessage());
            throw new CustomException(ExceptionCode.FFMPEG_ERROR, ex);
        }
    }

    private boolean isClientAbort(Throwable ex) {
        while (ex != null) {
            if (ex instanceof org.apache.catalina.connector.ClientAbortException
                    || (ex.getMessage() != null && ex.getMessage().contains("Broken pipe"))) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }
}
