package com.seohamin.fshs.v2.global.infra.ffmpeg;

import com.seohamin.fshs.v2.global.config.FfmpegConfig;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.ffmpeg.dto.FfmpegAnalysisResultDto;
import com.seohamin.fshs.v2.global.infra.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class FfmpegProcessor {

    private final FfmpegConfig ffmpegConfig;
    private final JsonMapper jsonMapper;

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

        final List<String> command = new java.util.ArrayList<>();
        command.add(ffmpegConfig.getFfmpeg());
        command.add("-loglevel");
        command.add("error");
        command.add("-ss");
        command.add(String.valueOf(start));
        command.add("-i");
        command.add(filePath);
        command.addAll(encoderOpts);
        command.addAll(List.of(
                "-pix_fmt", "yuv420p",
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
