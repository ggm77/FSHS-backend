package com.seohamin.fshs.v2.global.infra.storage;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.dto.FilePropertiesDto;
import com.seohamin.fshs.v2.global.util.MimeTypeUtil;
import com.seohamin.fshs.v2.global.util.storage.FileCategoryUtil;
import com.seohamin.fshs.v2.global.util.storage.PathNameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

/**
 * 파일이나 폴더의 저장, 조회, 이동, 삭제 등을 제공하는 클래스
 * 이 클래스를 통해서 서비스 레이어에서 파일 작업을 함
 */
@Component
@RequiredArgsConstructor
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

        // 5) 저장할 경로 생성
        final Path path = toAbsolutePath(tempPath, Path.of(name));

        // 6) 임시 폴더에 저장
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
        final Path path = internalMove(rawTempPath, tempPath, rawSavePath, rootPath);

        // 3) 마지막 수정 시점 수정
        final Path absPath = toAbsolutePath(rootPath, path);
        storageIoCore.updateLastModified(absPath, lastModified);

        return path;
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

        // 2) 파일명과 확장자 추출
        final String name = PathNameUtil.normalize(PathNameUtil.extractFileNameFromPath(path)).toLowerCase();
        final String baseName = PathNameUtil.extractBaseName(name);
        final String extension = PathNameUtil.extractExtension(name);

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
        return internalMove(rawSource, rootPath, rawDest, rootPath);
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

    /**
     * 실제로 파일 이동을 시키는 메서드
     * @param rawSource 이동 시킬 파일 상대 경로
     * @param sourceBase 루트 또는 임시 폴더 절대 경로
     * @param rawDest 이동할 목적지 폴더 상대 경로
     * @param destBase 루트 또는 임시 폴더 절대 경로
     * @return 이동한 파일의 상대 경로
     */
    private Path internalMove(
            final Path rawSource,
            final String sourceBase,
            final Path rawDest,
            final String destBase
    ) {
        // 1) null 검사 및 절대 경로로 변환
        final Path source = toAbsolutePath(sourceBase, rawSource);
        final Path dest = toAbsolutePath(destBase, rawDest);

        // 2) 이름 추출
        final String name = PathNameUtil.extractFileNameFromPath(source);

        // 3) 최종 저장 경로 생성
        final Path targetPath = dest.resolve(name);

        // 4) 이동
        storageIoCore.move(source, targetPath);

        // 5) root 경로를 통해 상대 경로로 변환
        final Path root = Path.of(rootPath).toAbsolutePath().normalize();
        return root.relativize(targetPath);
    }
}
