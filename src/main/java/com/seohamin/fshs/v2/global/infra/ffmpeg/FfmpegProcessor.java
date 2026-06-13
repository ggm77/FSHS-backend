package com.seohamin.fshs.v2.global.infra.ffmpeg;

import com.seohamin.fshs.v2.global.config.FfmpegConfig;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.ffmpeg.dto.FfmpegAnalysisResultDto;
import com.seohamin.fshs.v2.global.infra.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class FfmpegProcessor {

    private final FfmpegConfig ffmpegConfig;
    private final JsonMapper jsonMapper;

    @Value("${ffmpeg.max-concurrent:4}")
    private int maxConcurrent;

    private Semaphore transcodeSlots;

    @PostConstruct
    private void init() {
        transcodeSlots = new Semaphore(maxConcurrent);
    }

    // HLS 세그먼트 한 개의 길이(초)
    private static final int HLS_SEGMENT_SECONDS = 6;
    private static final int VIDEO_THUMBNAIL_TIMEOUT_SECONDS = 60;

    private static final String MAX_OUTPUT_RESOLUTION_FILTER =
            "scale=w=1920:h=1080:force_original_aspect_ratio=decrease:force_divisible_by=2";

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
        if (!transcodeSlots.tryAcquire()) {
            throw new CustomException(ExceptionCode.TRANSCODE_CAPACITY_EXCEEDED);
        }

        final List<String> command = buildVideoStreamCommand(filePath, start);

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
                transcodeSlots.release();
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

    List<String> buildVideoStreamCommand(
            final String filePath,
            final double start
    ) {
        final List<String> command = new java.util.ArrayList<>();
        command.add(ffmpegConfig.getFfmpeg());
        command.add("-loglevel");
        command.add("error");
        command.addAll(getHwAccelOptions());
        command.add("-ss");
        command.add(String.valueOf(start));
        command.add("-i");
        command.add(filePath);
        command.add("-vf");
        command.add(MAX_OUTPUT_RESOLUTION_FILTER);
        command.addAll(getEncoderOptions(ffmpegConfig.getSelectedH264Encoder()));
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.addAll(List.of(
                "-acodec", "aac",
                "-b:a", "128k",
                "-f", "mp4",
                "-movflags", "frag_keyframe+empty_moov+default_base_moof+omit_tfhd_offset",
                "-flush_packets", "1",
                "pipe:1"
        ));
        return command;
    }

    /**
     * 영상 길이를 기반으로 HLS VOD 재생목록(.m3u8)을 즉시 생성하는 메서드
     * DB에서 전달받은 재생 길이로 고정 길이 세그먼트 재생목록을 생성한다
     * @param durationSeconds 파일 재생 길이(초)
     * @return 재생목록이 담긴 InputStreamResource
     */
    public InputStreamResource getHlsPlaylist(final double durationSeconds) {
        final int fullSegments = (int) (durationSeconds / HLS_SEGMENT_SECONDS);
        final double remainder = durationSeconds - ((double) fullSegments * HLS_SEGMENT_SECONDS);

        final StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:3\n");
        sb.append("#EXT-X-TARGETDURATION:").append(HLS_SEGMENT_SECONDS).append("\n");
        sb.append("#EXT-X-MEDIA-SEQUENCE:0\n");
        sb.append("#EXT-X-PLAYLIST-TYPE:VOD\n");

        for (int i = 0; i < fullSegments; i++) {
            sb.append("#EXTINF:")
                    .append(String.format(Locale.ROOT, "%.3f", (double) HLS_SEGMENT_SECONDS))
                    .append(",\n");
            sb.append("segment").append(i).append(".ts\n");
        }
        // 마지막 자투리 구간 (영상 길이가 세그먼트로 딱 나눠떨어지지 않을 때)
        if (remainder > 0.001) {
            sb.append("#EXTINF:")
                    .append(String.format(Locale.ROOT, "%.3f", remainder))
                    .append(",\n");
            sb.append("segment").append(fullSegments).append(".ts\n");
        }
        sb.append("#EXT-X-ENDLIST\n");

        return new InputStreamResource(
                new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 요청된 인덱스의 HLS 세그먼트(.ts)를 실시간 트랜스코딩하는 메서드
     * 해당 구간만 잘라 h264 / aac mpeg-ts로 변환한다
     * 세그먼트별로 PTS가 타임라인 상 제 위치를 갖도록 -output_ts_offset으로 보정한다
     * @param filePath 파일 절대 경로
     * @param segmentIndex 세그먼트 인덱스 (0부터 시작)
     * @return 트랜스코딩된 세그먼트가 담긴 InputStreamResource
     */
    public InputStreamResource getHlsSegment(
            final Path filePath,
            final int segmentIndex
    ) {
        if (!transcodeSlots.tryAcquire()) {
            throw new CustomException(ExceptionCode.TRANSCODE_CAPACITY_EXCEEDED);
        }
        try {
            final double start = (double) segmentIndex * HLS_SEGMENT_SECONDS;
            final List<String> command = buildHlsSegmentCommand(filePath, start);
            return new InputStreamResource(new ByteArrayInputStream(executeBinary(command, 60)));
        } finally {
            transcodeSlots.release();
        }
    }

    List<String> buildHlsSegmentCommand(
            final Path filePath,
            final double start
    ) {
        final List<String> command = new java.util.ArrayList<>();
        command.add(ffmpegConfig.getFfmpeg());
        command.add("-loglevel");
        command.add("error");
        command.addAll(getHwAccelOptions());
        command.add("-ss");
        command.add(String.valueOf(start));
        command.add("-i");
        command.add(filePath.toString());
        command.add("-t");
        command.add(String.valueOf(HLS_SEGMENT_SECONDS));
        command.add("-vf");
        command.add(MAX_OUTPUT_RESOLUTION_FILTER);
        command.addAll(getEncoderOptions(ffmpegConfig.getSelectedH264Encoder()));
        command.add("-force_key_frames");
        command.add("expr:gte(t," + start + ")");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.addAll(List.of(
                "-acodec", "aac",
                "-b:a", "128k",
                "-muxdelay", "0",
                "-muxpreload", "0",
                "-f", "mpegts",
                "pipe:1"
        ));
        return command;
    }

    /**
     * 동영상 첫 프레임을 JPEG 썸네일로 생성하는 메서드
     * @param filePath 원본 동영상 절대 경로
     * @param targetPath 생성할 썸네일 절대 경로
     * @param thumbnailMaxSize 썸네일 최대 가로/세로 크기
     */
    public void createVideoThumbnail(
            final Path filePath,
            final Path targetPath,
            final int thumbnailMaxSize
    ) {
        if (filePath == null || targetPath == null || thumbnailMaxSize <= 0) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        executeAndRequireSuccess(
                buildVideoThumbnailCommand(filePath, targetPath, thumbnailMaxSize),
                VIDEO_THUMBNAIL_TIMEOUT_SECONDS
        );
    }

    List<String> buildVideoThumbnailCommand(
            final Path filePath,
            final Path targetPath,
            final int thumbnailMaxSize
    ) {
        final List<String> command = new java.util.ArrayList<>();
        command.add(ffmpegConfig.getFfmpeg());
        command.add("-loglevel");
        command.add("error");
        command.add("-y");
        command.addAll(getHwAccelOptions());
        command.add("-i");
        command.add(filePath.toString());
        command.add("-map");
        command.add("0:v:0");
        command.add("-frames:v");
        command.add("1");
        command.add("-vf");
        command.add("scale=w=" + thumbnailMaxSize + ":h=" + thumbnailMaxSize
                + ":force_original_aspect_ratio=decrease");
        command.add("-q:v");
        command.add("3");
        command.add(targetPath.toString());
        return command;
    }

    /**
     * 분석 결과에서 영상 길이(초)를 파싱하는 메서드
     * @param analysis ffprobe 분석 결과
     * @return 영상 길이 (초)
     */
    private double parseDurationSeconds(final FfmpegAnalysisResultDto analysis) {
        if (analysis == null || analysis.format() == null
                || analysis.format().duration() == null || analysis.format().duration().isBlank()) {
            throw new CustomException(ExceptionCode.FFMPEG_ERROR);
        }
        try {
            return Double.parseDouble(analysis.format().duration());
        } catch (final NumberFormatException ex) {
            throw new CustomException(ExceptionCode.FFMPEG_ERROR, ex);
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

    private List<String> getHwAccelOptions() {
        final String api = ffmpegConfig.getSelectedHwAccelApi();
        if (api == null || api.isBlank()) {
            return List.of();
        }
        return List.of("-hwaccel", api);
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

    /**
     * FFmpeg 명령어를 실행해 표준 출력(stdout) 바이너리를 통째로 읽어오는 메서드
     * 길이가 제한된(-t) 세그먼트 변환처럼 출력이 유한할 때 사용한다
     * @param command 실행할 명령어
     * @param timeoutSecond 타임아웃 시간 (초)
     * @return stdout 바이너리
     */
    private byte[] executeBinary(
            final List<String> command,
            final int timeoutSecond
    ) {
        final ProcessBuilder pb = new ProcessBuilder(command);

        try {
            final Process p = pb.start();

            // 에러 스트림(stderr)을 비동기적으로 읽어 버퍼 블로킹 방지 및 에러 로깅
            final Thread errorReader = Thread.ofPlatform().start(() -> {
                try (final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error("[FFmpeg 프로세스 에러] {}", line);
                    }
                } catch (final Exception ignored) {}
            });

            // stdout을 EOF까지 읽음 — 출력이 -t로 제한되어 유한함
            final byte[] data;
            try (final InputStream is = p.getInputStream()) {
                data = is.readAllBytes();
            }

            if (!p.waitFor(timeoutSecond, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.error("[FFmpeg 타임아웃] - 명령어: {}", command);
                throw new CustomException(ExceptionCode.COMMAND_TIMEOUT);
            }

            errorReader.join(1000);
            return data;
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

    private void executeAndRequireSuccess(
            final List<String> command,
            final int timeoutSecond
    ) {
        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            final Process process = pb.start();
            final StringBuilder output = new StringBuilder();
            final Thread readerThread = Thread.ofPlatform().start(() -> {
                try (final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines().forEach(line -> output.append(line).append("\n"));
                } catch (final java.io.IOException ignored) {}
            });

            if (!process.waitFor(timeoutSecond, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.error("[FFmpeg 타임아웃] - 명령어: {}", command);
                throw new CustomException(ExceptionCode.COMMAND_TIMEOUT);
            }

            readerThread.join(1000);
            if (process.exitValue() != 0) {
                log.error("[FFmpeg 에러 발생] - exitCode={}, output={}", process.exitValue(), output);
                throw new CustomException(ExceptionCode.FFMPEG_ERROR);
            }
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
