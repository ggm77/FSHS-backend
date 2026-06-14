package com.seohamin.fshs.v2.global.infra.storage;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.dto.FilePropertiesDto;
import com.seohamin.fshs.v2.global.util.MimeTypeUtil;
import com.seohamin.fshs.v2.global.util.storage.FileCategoryUtil;
import com.seohamin.fshs.v2.global.util.storage.PathNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 파일이나 폴더의 저장, 조회, 이동, 삭제 등을 제공하는 클래스
 * 이 클래스를 통해서 서비스 레이어에서 파일 작업을 함
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorageManager {

    @Value("${fshs.storage.data-path}")
    private String rootPath;

    @Value("${fshs.storage.temp-path}")
    private String tempPath;

    private final StorageIoCore storageIoCore;

    /**
     * 업로드 된 파일을 임시 폴더에 저장하는 메서드
     * @param multipartFile 업로드 된 파일
     * @return 임시 폴더에 저장된 파일의 절대 경로
     */
    public Path saveTemporarily(final MultipartFile multipartFile) {
        // 1) null 체크
        if (multipartFile == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 이름 추출
        final String rawFileName = multipartFile.getOriginalFilename();

        // 3) 파일명 검사
        if (rawFileName == null || rawFileName.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 4) 프론트가 준 파일명 NFC 변환 및 실제 파일명만 추출 (오래된 브라우저 대비)
        final String name = PathNameUtil.normalize(PathNameUtil.extractFileName(rawFileName));

        // 5) UUID 서브디렉토리 생성 — 동일 파일명 동시 업로드 시 충돌 방지
        final Path tempSubDir = toAbsolutePath(tempPath, Path.of(UUID.randomUUID().toString()));
        storageIoCore.createFolder(tempSubDir);

        // 6) 임시 폴더에 저장
        final Path path = tempSubDir.resolve(name);
        storageIoCore.write(multipartFile, path);

        return path;
    }

    /**
     * 임시 폴더에 저장된 파일을 실제 장소로 옮기는 메서드
     * @param rawTempPath 임시 파일 상대 경로
     * @param rawSavePath 저장할 폴더 상대 경로
     * @return 저장된 파일 상대 경로
     */
    public Path savePermanently(
            final Path rawTempPath,
            final Path rawSavePath,
            final Instant lastModified
    ) {
        // 1) null 체크
        if (rawTempPath == null || rawSavePath == null || lastModified == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 실제 위치로 이동
        final Path path = internalFileMove(rawTempPath, tempPath, rawSavePath, rootPath);

        // 3) 마지막 수정 시점 수정
        final Path absPath = toAbsolutePath(rootPath, path);
        storageIoCore.updateLastModified(absPath, lastModified);

        // 4) 파일 이동 완료 후 빈 UUID 서브디렉토리 정리
        cleanupTempParent(rawTempPath);

        return path;
    }

    /**
     * 임시 파일과 업로드별 UUID 서브디렉토리를 정리하는 메서드
     * @param rawTempPath 정리할 임시 파일 경로
     */
    public void deleteTemporaryFile(final Path rawTempPath) {
        if (rawTempPath == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        final Path tempFilePath = toAbsolutePath(tempPath, rawTempPath);
        try {
            Files.deleteIfExists(tempFilePath);
        } catch (final IOException ex) {
            log.warn("[임시 파일 정리 실패]: {}", tempFilePath, ex);
        }
        cleanupTempParent(rawTempPath);
    }

    /**
     * 파일에서 상세 속성을 가져오는 메서드
     * @param path 상세 속성 가져올 파일 절대 경로
     * @return 상세 속성 담긴 DTO
     */
    public FilePropertiesDto getFileProperties(final Path path) {
        // 1) null 검사
        if (path == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 파일명과 확장자 추출 (name은 원본 케이스 유지, 파생 값은 소문자 기준)
        final String name = PathNameUtil.normalize(PathNameUtil.extractFileNameFromPath(path));
        final String lowerName = name.toLowerCase();
        final String baseName = PathNameUtil.extractBaseName(lowerName);
        final String extension = PathNameUtil.extractExtension(lowerName);

        // 3) 파일 카테고리 분류
        final Category category = FileCategoryUtil.categorize(extension);

        // 4) MIME Type 확인 - Map에 정보 없으면 OS에 존재하는 정보로 찾음
        String mimeType = MimeTypeUtil.getMimeType(extension);
        if (mimeType == null) {
            mimeType = storageIoCore.getMimeType(path);
        }

        // 5) 파일 상세 속성 확인
        final BasicFileAttributes attrs = storageIoCore.readAttributes(path);

        // 6) DTO 조립
        return new FilePropertiesDto(
                mimeType,
                attrs.size(),
                category,
                name,
                baseName,
                extension
        );
    }

    /**
     * 파일을 조회해서 Resource로 가져오는 메서드
     * @param path 파일의 상대경로
     * @return Resource 객체
     */
    public Resource getFile(final Path path) {
        // 1) null 검사
        if (path == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 절대 경로로 변환
        final Path absPath = toAbsolutePath(rootPath, path);

        // 3) 파일 읽어와서 리턴
        return storageIoCore.readFileAsResource(absPath);
    }

    /**
     * 파일을 이동시키는 메서드
     * @param rawSource 이동시킬 파일 경로
     * @param rawDest 목적지 폴더 경로
     * @return 옮겨진 파일의 경로
     */
    public Path moveFile(
            final Path rawSource,
            final Path rawDest
    ) {
        return internalFileMove(rawSource, rootPath, rawDest, rootPath);
    }

    /**
     * 폴더를 이동시키는 메서드 (이동/이름 변경 공용)
     * 파일 이동과 달리 rawDest는 목적지 부모가 아니라 폴더명까지 포함한 최종 경로다
     * @param rawSource 이동시킬 폴더 경로
     * @param rawDest 폴더명을 포함한 최종 목적지 경로
     * @return 옮겨진 폴더의 상대 경로
     */
    public Path moveFolder(
            final Path rawSource,
            final Path rawDest
    ) {
        return internalFolderMove(rawSource, rootPath, rawDest, rootPath);
    }

    /**
     * 폴더 생성하는 메서드
     * @param rawPath 폴더 생성할 상대 경로
     * @return 생성된 폴더의 상대 경로
     */
    public Path createFolder(final Path rawPath) {
        // 1) null 검사
        if (rawPath == null) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 2) 정규화
        final Path path = PathNameUtil.normalizePath(rawPath);

        // 3) 절대 경로로 변환
        final Path absolutePath = toAbsolutePath(rootPath, path);

        // 4) 폴더 생성
        storageIoCore.createFolder(absolutePath);

        return path;
    }

    /**
     * 상대 경로를 절대 경로로 바꿔주는 메서드이다.
     * 임시 파일의 경로가 아닌 경우, 일반 루트 폴더 경로를 붙여줌
     * @param rawRelativePath 상대 경로
     * @param isTempFilePath 임시 파일인지 여부
     * @return Path로 된 절대 경로
     */
    public Path resolvePath(
            final String rawRelativePath,
            final boolean isTempFilePath
    ) {
        final Path relativePath = Path.of(rawRelativePath);

        if (isTempFilePath) {
            return resolveExistingPathIfPossible(tempPath, relativePath);
        } else {
            return resolveExistingPathIfPossible(rootPath, relativePath);
        }

    }

    /**
     * 파일 삭제하는 메서드
     * @param pathStr 삭제할 파일의 경로
     */
    public void removeFile(final String pathStr) {
        // 1) null 검사
        if (pathStr == null || pathStr.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 절대 경로로 변환
        final Path path = resolveExistingPathIfPossible(rootPath, Path.of(pathStr));

        // 3) 추후 휴지통 기능 구현하기 위해 메서드로 분리
        deleteFile(path);
    }

    /**
     * 폴더 삭제하는 메서드
     * @param pathStr 삭제할 폴더의 경로
     */
    public void removeFolder(final String pathStr) {
        // 1) null 검사
        if (pathStr == null || pathStr.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 2) 절대 경로로 변환
        final Path path = resolveExistingPathIfPossible(rootPath, Path.of(pathStr));

        // 3) 휴지통 기능 구현을 위한 메서드 분리
        deleteFolder(path);
    }

    /**
     * 상대 경로를 루트 경로 또는 임시 폴더 경로를 포함한
     * 절대 경로로 변환하는 메서드
     * @param base 루트 또는 임시 폴더 경로
     * @param rawPath 상대 경로
     * @return 절대 경로
     */
    private Path toAbsolutePath(
            final String base,
            final Path rawPath
    ) {
        // 1) null 검사
        if (rawPath == null) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 2) base랑 합쳐서 절대 경로로
        final Path basePath = Path.of(base).toAbsolutePath().normalize();
        final Path absolutePath = basePath.resolve(rawPath).normalize();

        // 4) 경로 검사
        if (!absolutePath.startsWith(basePath)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        return absolutePath;
    }

    private Path resolveExistingPathIfPossible(
            final String base,
            final Path rawPath
    ) {
        final Path requestedPath = toAbsolutePath(base, rawPath);
        if (Files.exists(requestedPath)) {
            return requestedPath;
        }

        final Path basePath = Path.of(base).toAbsolutePath().normalize();
        final Path relativePath = basePath.relativize(requestedPath);
        Path current = basePath;
        for (final Path name : relativePath) {
            final Path exactPath = current.resolve(name);
            if (Files.exists(exactPath)) {
                current = exactPath;
                continue;
            }
            if (!Files.isDirectory(current)) {
                return requestedPath;
            }

            final String normalizedName = PathNameUtil.normalize(name.toString());
            try (Stream<Path> children = Files.list(current)) {
                final List<Path> matches = children
                        .filter(child -> PathNameUtil.normalize(child.getFileName().toString()).equals(normalizedName))
                        .toList();
                if (matches.size() != 1) {
                    return requestedPath;
                }
                current = matches.getFirst();
            } catch (final IOException ex) {
                return requestedPath;
            }
        }

        if (!current.normalize().startsWith(basePath)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }
        return current.normalize();
    }

    /**
     * 실제로 파일 이동을 시키는 메서드
     * @param rawSource 이동 시킬 파일 상대 경로
     * @param sourceBase 루트 또는 임시 폴더 절대 경로
     * @param rawDest 이동할 목적지 폴더 상대 경로
     * @param destBase 루트 또는 임시 폴더 절대 경로
     * @return 이동한 파일의 상대 경로
     */
    private Path internalFileMove(
            final Path rawSource,
            final String sourceBase,
            final Path rawDest,
            final String destBase
    ) {
        // 1) null 검사 및 절대 경로로 변환
        final Path source = resolveExistingPathIfPossible(sourceBase, rawSource);
        final Path dest = toAbsolutePath(destBase, rawDest);

        // 2) 이름 추출
        final String name = PathNameUtil.normalize(PathNameUtil.extractFileNameFromPath(rawSource));

        // 3) 최종 저장 경로 생성
        final Path targetPath = dest.resolve(name);

        // 4) 이동
        storageIoCore.move(source, targetPath);

        // 5) root 경로를 통해 상대 경로로 변환
        final Path root = Path.of(rootPath).toAbsolutePath().normalize();
        return root.relativize(targetPath);
    }

    private void cleanupTempParent(final Path rawTempPath) {
        final Path parent = toAbsolutePath(tempPath, rawTempPath).getParent();
        if (parent == null) return;
        try {
            Files.deleteIfExists(parent);
        } catch (final IOException ex) {
            log.warn("[임시 디렉토리 정리 실패]: {}", parent, ex);
        }
    }

    /**
     * 실제로 폴더 이동을 시키는 메서드
     * @param rawSource 이동 시킬 폴더 상대 경로
     * @param sourceBase 루트 또는 임시 폴더 절대 경로
     * @param rawDest 이동할 목적지 폴더 상대 경로
     * @param destBase 루트 또는 임시 폴더 절대 경로
     * @return 이동한 폴더의 상대 경로
     */
    private Path internalFolderMove(
            final Path rawSource,
            final String sourceBase,
            final Path rawDest,
            final String destBase
    ) {
        // 1) null 검사 및 절대 경로로 변환
        final Path source = resolveExistingPathIfPossible(sourceBase, rawSource);
        final Path dest = toAbsolutePath(destBase, rawDest);

        // 2) 이동 (rawDest가 폴더명을 포함한 최종 경로이므로 파일과 달리 이름을 다시 붙이지 않음)
        storageIoCore.move(source, dest);

        // 3) root 경로를 통해 상대 경로로 변환
        final Path root = Path.of(rootPath).toAbsolutePath().normalize();
        return root.relativize(dest);
    }

    /**
     * 파일 삭제하는 메서드
     * @param path 삭제할 파일 path
     */
    private void deleteFile(final Path path) {
        // 1) null 검사
        if (path == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 파일 삭제
        storageIoCore.deleteFile(path);
    }

    /**
     * 폴더 삭제하는 메서드
     * @param path 삭제할 폴더 path
     */
    private void deleteFolder(final Path path) {
        // 1) null 검사
        if (path == null) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 2) 폴더 삭제
        storageIoCore.deleteFolder(path);
    }
}
