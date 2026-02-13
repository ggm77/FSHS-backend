package com.seohamin.fshs.v2.global.infra.storage;

import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.util.MimeTypeUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 파일과 폴더의 입출력을 담당하는 클래스
 */
@Component
public class StorageIoCore {

    /**
     * 파일을 저장하는 메서드
     * @param multipartFile 저장할 파일
     * @param path 저장할 위치
     */
    public void write(
            final MultipartFile multipartFile,
            final Path path
    ) {
        try {
            multipartFile.transferTo(path);
        } catch (final FileAlreadyExistsException ex) {
            // 동일 파일명 존재 에러
            throw new CustomException(ExceptionCode.FILE_ALREADY_EXIST);
        } catch (final AccessDeniedException ex) {
            // 접근 권한 에러
            throw new CustomException(ExceptionCode.STORAGE_ACCESS_DENIED);
        } catch (final NoSuchFileException ex) {
            // 저장할 위치가 존재하지 않는 경우
            throw new CustomException(ExceptionCode.PATH_NOT_FOUND);
        } catch (final IOException ex) {
            final String message = ex.getMessage();

            // 저장공간 부족 에러
            if (message != null && (message.contains("No space left") || message.contains("not enough space"))) {
                throw new CustomException(ExceptionCode.STORAGE_FULL);
            }

            // 알 수 없는 에러
            else {
                throw new CustomException(ExceptionCode.FILE_WRITE_ERROR);
            }
        }
    }

    /**
     * 파일 이동하는 메서드
     * @param source 이동할 파일 경로
     * @param target 파일명이 포함된 목표 경로
     */
    public void move(
            final Path source,
            final Path target
    ) {
        try {
            Files.move(source, target);
        } catch (final FileAlreadyExistsException ex) {
            throw new CustomException(ExceptionCode.FILE_ALREADY_EXIST);
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FILE_WRITE_ERROR);
        }
    }

    /**
     * 폴더 생성하는 메서드
     * @param path 생성할 폴더 경로
     */
    public void createFolder(final Path path) {
        try {
            Files.createDirectory(path);
        } catch (final FileAlreadyExistsException ex) {
            throw new CustomException(ExceptionCode.FILE_ALREADY_EXIST);
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FOLDER_WRITE_ERROR);
        }
    }

    /**
     * 파일의 MIME Type을 Files.probeContentType를 통해 알아내는 메서드
     * 못찾으면 application/octet-stream 리턴
     * @param path 대상 파일 경로
     * @return MIME 타입
     */
    public String getMimeType(final Path path) {
        try {
            return Files.probeContentType(path);
        } catch (final IOException ex) {
            return MimeTypeUtil.DEFAULT_MIME_TYPE;
        }
    }

    /**
     * 파일의 상세 속성을 읽어오는 메서드
     * @param path 속성 읽어올 파일 경로
     * @return BasicFileAttributes
     */
    public BasicFileAttributes readAttributes(final Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class);
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FILE_READ_ERROR);
        }
    }
}
