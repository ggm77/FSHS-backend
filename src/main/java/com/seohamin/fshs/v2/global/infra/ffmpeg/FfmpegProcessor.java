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
import java.util.stream.Collectors;

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
        final List<String> command = List.of(
                ffmpegConfig.getFfmpeg(),
                "-loglevel", "quiet", // 출력에 영상 내용만 나오도록
                "-ss", String.valueOf(start), // 시작 지점
                "-i", filePath, // 파일 위치
                "-vcodec", "h264_videotoolbox", // h264로 변환
                "-preset", "ultrafast", // 인코딩 속도
                "-tune", "zerolatency", // 최적화 옵션
                "-pix_fmt", "yuv420p", // 픽셀 포멧
                "-acodec", "aac", // 오디오 코덱 설정
                "-b:a", "128k", // 오디오 비트 레이트 설정
                "-f", "mp4", // mp4로 형식 강제
                "-movflags", "frag_keyframe+empty_moov+default_base_moof", // 스트리밍을 위한 메타데이터 설정
                "pipe:1" // 표준 출력으로 출력
        );

        return outputStream -> {
            final ProcessBuilder pb = new ProcessBuilder(command);

            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            final Process process = pb.start();

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
            }
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

            String result;
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                result = reader.lines().collect(Collectors.joining("\n"));
            }

            if (!p.waitFor(timeoutSecond, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.error("[FFmpeg 타임아웃] - 명령어: {}", command);
                throw new CustomException(ExceptionCode.COMMAND_TIMEOUT);
            }

            return result;
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
