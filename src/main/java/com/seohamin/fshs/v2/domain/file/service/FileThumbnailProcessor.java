package com.seohamin.fshs.v2.domain.file.service;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.ffmpeg.FfmpegProcessor;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileThumbnailProcessor {

    private static final Set<Category> SUPPORTED_CATEGORIES = EnumSet.of(Category.IMAGE, Category.VIDEO);
    private static final int THUMBNAIL_MAX_SIZE = 480;
    private static final String THUMBNAIL_EXTENSION = ".jpg";

    private final StorageManager storageManager;
    private final FfmpegProcessor ffmpegProcessor;

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
            if (category == Category.IMAGE) {
                createImageThumbnail(fileUuid, sourcePath, targetPath);
            } else if (category == Category.VIDEO) {
                createVideoThumbnail(fileUuid, sourcePath, targetPath);
            }
            log.info("[썸네일 생성 완료]: uuid={}", fileUuid);
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

    private void createImageThumbnail(
            final String fileUuid,
            final Path sourcePath,
            final Path targetPath
    ) {
        final Path tempPath = resolveTempPath(fileUuid, targetPath);
        try {
            Thumbnails.of(sourcePath.toFile())
                    .size(THUMBNAIL_MAX_SIZE, THUMBNAIL_MAX_SIZE)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toFile(tempPath.toFile());
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (final Exception ex) {
            throw new CustomException(ExceptionCode.FILE_WRITE_ERROR, ex);
        } finally {
            deleteTempFile(tempPath);
        }
    }

    private void createVideoThumbnail(
            final String fileUuid,
            final Path sourcePath,
            final Path targetPath
    ) {
        final Path tempPath = resolveTempPath(fileUuid, targetPath);
        try {
            ffmpegProcessor.createVideoThumbnail(sourcePath, tempPath, THUMBNAIL_MAX_SIZE);
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (final CustomException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CustomException(ExceptionCode.FILE_WRITE_ERROR, ex);
        } finally {
            deleteTempFile(tempPath);
        }
    }

    private Path resolveTempPath(
            final String fileUuid,
            final Path targetPath
    ) {
        return targetPath.resolveSibling(fileUuid + ".tmp-" + UUID.randomUUID() + THUMBNAIL_EXTENSION);
    }

    private void deleteTempFile(final Path tempPath) {
        try {
            Files.deleteIfExists(tempPath);
        } catch (final Exception ex) {
            log.warn("[썸네일 임시 파일 정리 실패]: {}", tempPath, ex);
        }
    }
}
