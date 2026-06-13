package com.seohamin.fshs.v2.domain.file.service;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.config.FfmpegConfig;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileThumbnailProcessor {

    private static final Set<Category> SUPPORTED_CATEGORIES = EnumSet.of(Category.IMAGE, Category.VIDEO);
    private static final int THUMBNAIL_MAX_SIZE = 480;
    private static final int COMMAND_TIMEOUT_SECONDS = 60;
    private static final String THUMBNAIL_EXTENSION = ".jpg";

    private final StorageManager storageManager;
    private final FfmpegConfig ffmpegConfig;

    @Value("${fshs.storage.thumbnail-path}")
    private String thumbnailPath;

    /**
     * 저장 완료된 파일의 썸네일을 비동기로 생성한다.
     * 썸네일 생성 실패는 원본 파일 처리 결과에 영향을 주지 않도록 내부에서 로깅 후 종료한다.
     * @param fileUuid 파일 UUID
     * @param relativePath 원본 파일 상대 경로
     * @param category 파일 카테고리
     */
    @Async("asyncExecutor")
    public void process(
            final String fileUuid,
            final String relativePath,
            final Category category
    ) {
        try {
            if (!isValidRequest(fileUuid, relativePath, category)) {
                log.warn("[썸네일 생성 요청 무시]: uuid={}, path={}, category={}",
                        fileUuid, relativePath, category);
                return;
            }

            if (!supports(category)) {
                return;
            }

            final Path sourcePath = storageManager.resolvePath(relativePath, false);
            final Path targetPath = resolveThumbnailPath(fileUuid);
            if (Files.exists(targetPath)) {
                return;
            }
            if (Files.notExists(sourcePath) || !Files.isRegularFile(sourcePath)) {
                log.warn("[썸네일 원본 파일 없음]: uuid={}, path={}", fileUuid, sourcePath);
                return;
            }

            Files.createDirectories(targetPath.getParent());
            final Path tempPath = targetPath.resolveSibling(fileUuid + ".tmp-" + UUID.randomUUID() + THUMBNAIL_EXTENSION);
            try {
                execute(buildThumbnailCommand(sourcePath, tempPath), COMMAND_TIMEOUT_SECONDS);
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("[썸네일 생성 완료]: uuid={}", fileUuid);
            } finally {
                Files.deleteIfExists(tempPath);
            }
        } catch (final Exception ex) {
            log.warn("[썸네일 생성 실패]: uuid={}, path={}, message={}",
                    fileUuid, relativePath, ex.getMessage(), ex);
        }
    }

    public boolean supports(final Category category) {
        return SUPPORTED_CATEGORIES.contains(category);
    }

    public Path resolveThumbnailPath(final String fileUuid) {
        if (fileUuid == null || fileUuid.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final Path basePath = Path.of(thumbnailPath).toAbsolutePath().normalize();
        final Path targetPath = basePath.resolve(fileUuid + THUMBNAIL_EXTENSION).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }
        return targetPath;
    }

    private boolean isValidRequest(
            final String fileUuid,
            final String relativePath,
            final Category category
    ) {
        return fileUuid != null && !fileUuid.isBlank()
                && relativePath != null && !relativePath.isBlank()
                && category != null;
    }

    private List<String> buildThumbnailCommand(
            final Path sourcePath,
            final Path targetPath
    ) {
        return List.of(
                ffmpegConfig.getFfmpeg(),
                "-loglevel", "error",
                "-y",
                "-i", sourcePath.toString(),
                "-map", "0:v:0",
                "-frames:v", "1",
                "-vf", "scale=w=" + THUMBNAIL_MAX_SIZE
                        + ":h=" + THUMBNAIL_MAX_SIZE
                        + ":force_original_aspect_ratio=decrease",
                "-q:v", "3",
                targetPath.toString()
        );
    }

    private void execute(
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
                } catch (final Exception ignored) {}
            });

            if (!process.waitFor(timeoutSecond, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.error("[썸네일 생성 타임아웃] - 명령어: {}", command);
                throw new CustomException(ExceptionCode.COMMAND_TIMEOUT);
            }

            readerThread.join(1000);
            if (process.exitValue() != 0) {
                log.error("[썸네일 생성 FFmpeg 실패] - exitCode={}, output={}",
                        process.exitValue(), output);
                throw new CustomException(ExceptionCode.FFMPEG_ERROR);
            }
        } catch (final CustomException ex) {
            throw ex;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CustomException(ExceptionCode.PROCESS_INTERRUPTED);
        } catch (final Exception ex) {
            log.error("[썸네일 생성 FFmpeg 에러] {}", ex.getMessage());
            throw new CustomException(ExceptionCode.FFMPEG_ERROR, ex);
        }
    }
}
