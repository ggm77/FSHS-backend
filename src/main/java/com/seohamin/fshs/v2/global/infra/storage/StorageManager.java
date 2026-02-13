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
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 파일이나 폴더의 저장, 조회, 이동, 삭제 등을 제공하는 클래스
 * 이 클래스를 통해서 서비스 레이어에서 파일 작업을 함
 */
@Component
@RequiredArgsConstructor
public class StorageManager {

    @Value("${file.root-path}")
    private String rootPath;

    @Value("${file.upload.temp-path}")
    private String tempPath;

    private final StorageIoCore storageIoCore;

    /**
     * 업로드 된 파일을 임시 폴더에 저장하는 메서드
     * @param multipartFile 업로드 된 파일
     * @return 임시 폴더에 저장된 파일의 경로
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
        final Path path = createPath(tempPath, name);

        // 6) 임시 폴더에 저장
        storageIoCore.write(multipartFile, path);

        return path;
    }

    /**
     * 파일에서 상세 속성을 가져오는 메서드
     * @param path 상세 속성 가져올 파일 경로
     * @return 상세 속성 담긴 DTO
     */
    public FilePropertiesDto getFileProperties(final Path path) {
        // 1) null 검사
        if (path == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 파일명과 확장자 추출
        final String name = PathNameUtil.normalize(PathNameUtil.extractFileNameFromPath(path));
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
                attrs.creationTime().toInstant(),
                attrs.lastModifiedTime().toInstant(),
                category,
                name,
                baseName,
                extension
        );
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
        // 1) null 검사
        if (rawSource == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }
        if (rawDest == null) {
            throw new CustomException(ExceptionCode.FOLDER_NOT_EXIST);
        }

        // 2) NFC 변환
        final Path source = PathNameUtil.normalizePath(rawSource);
        final Path dest = PathNameUtil.normalizePath(rawDest);

        // 2) 파일명 추출
        final String name = PathNameUtil.extractFileNameFromPath(source);

        // 3) 최종 목적지 경로 생성
        final Path targetPath = dest.resolve(name);

        // 4) 경로 검사
        validatePathWithinRoot(source);
        validatePathWithinRoot(targetPath);

        // 5) 파일 이동
        storageIoCore.move(source, targetPath);

        return targetPath;
    }

    /**
     * 상위 폴더 경로와 파일의 이름을 통해 경로를 생성하는 메서드
     * 동시에 경로 검사도 진행함.
     * @param parentFolderPathStr 상위 폴더의 상대 위치
     * @param fileNameStr 파일의 확장자를 포함한 이름
     * @return 파일의 전체 상대 경로
     */
    private Path createPath(
            final String parentFolderPathStr,
            final String fileNameStr
    ) {
        // 1) null 검사
        if (parentFolderPathStr == null || fileNameStr == null) {
            throw new CustomException(ExceptionCode.PATH_NOT_FOUND);
        }

        // 2) Path로 만들기
        final Path root = Path.of(rootPath).toAbsolutePath().normalize();
        final Path parentFolderPath = root.resolve(parentFolderPathStr).normalize();
        final Path targetPath = parentFolderPath.resolve(fileNameStr).normalize();

        // 3) 경로 검사
        if (!parentFolderPath.startsWith(root) || !targetPath.startsWith(parentFolderPath)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        return targetPath;
    }

    /**
     * Path가 루트 폴더를 벗어나진 않았는지 확인하는 메서드
     * @param targetPath 확인할 경로
     */
    private void validatePathWithinRoot(final Path targetPath) {
        // 1) 절대 경로 만들기
        final Path root = Path.of(rootPath).toAbsolutePath().normalize();
        final Path absoluteTarget = targetPath.toAbsolutePath().normalize();

        // 2) 경로 검사
        if (!absoluteTarget.startsWith(root)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }
    }
}
