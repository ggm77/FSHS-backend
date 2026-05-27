package com.seohamin.fshs.v2.domain.file.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileStatusResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileUploadResponseDto;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.entity.Status;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.ffmpeg.FfmpegProcessor;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final StorageManager storageManager;
    private final FileUploadProcessor fileUploadProcessor;
    private final Cache<Long, String> filePathCache;
    private final Cache<String, Status> fileStatusCache;
    private final FfmpegProcessor ffmpegProcessor;

    /**
     * 파일 하나 업로드처리하는 메서드
     *
     * @param multipartFile 업로드할 파일
     * @param lastModified 업로드할 파일의 마지막 수정 시점
     * @return 업로드된 파일의 정보
     */
    public FileUploadResponseDto uploadFile(
            final MultipartFile multipartFile,
            final Instant lastModified,
            final Long folderId
    ) {

        // 1) null 검사
        if (multipartFile == null || lastModified == null || folderId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 시스템 루트 검사
        if (folderId == 1L) {
            throw new CustomException(ExceptionCode.SYSTEM_ROOT_FORBIDDEN);
        }

        // 3) 상위 폴더 존재 확인 - 잘못된 folderId는 동기 단계에서 즉시 거절
        if (!folderRepository.existsById(folderId)) {
            throw new CustomException(ExceptionCode.FOLDER_NOT_EXIST);
        }

        // 4) 임시 폴더에 파일 저장 - MultipartFile 수명 문제 때문에 요청 스레드에서 동기로 처리
        final Path tempFilePath = storageManager.saveTemporarily(multipartFile);

        // 5) 파일 UUID 발급 및 처리 중 상태 등록
        final String fileUuid = UUID.randomUUID().toString();
        fileStatusCache.put(fileUuid, Status.PROCESSING);

        // 6) 비동기 처리 위임
        fileUploadProcessor.process(fileUuid, tempFilePath, folderId, lastModified);

        return new FileUploadResponseDto(fileUuid);
    }

    /**
     * 파일의 처리 상태를 조회하는 API
     * 캐시에 없으면 DB 조회
     * @param fileUuid 파일의 UUID
     * @return 파일 처리 상태 담긴 DTO
     */
    public FileStatusResponseDto getFileStatus(final String fileUuid) {
        // 1) null 검사
        if (fileUuid == null || fileUuid.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 캐시 조회
        final Status status = fileStatusCache.get(
                fileUuid,
                f -> fileRepository.existsByUuid(fileUuid) ? Status.COMPLETE : Status.ERROR
        );

        // 3) 파일 완료 되었으면 파일 아이디 조회
        final File file = fileRepository.findByUuid(fileUuid)
                .orElse(null);

        final Long id;
        if (file == null) {
            id = null;
        } else {
            id = file.getId();
        }

        return new FileStatusResponseDto(status, id);
    }

    /**
     * 파일의 정보를 조회하는 메서드
     * @param fileId 조회할 파일 ID
     * @return 파일 정보 담긴 DTO
     */
    public FileResponseDto getFileDetails(final Long fileId) {
        // 1) null 검사
        if(fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 정보 조회
        final File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));

        // 3) DTO에 담기
        return FileResponseDto.of(file);
    }


    /**
     * 파일을 다운로드 하거나 직접 스트리밍을 해주는 메서드
     * @param fileId 프론트로 전송할 파일의 ID
     * @return 파일의 정보가 담긴 DTO
     */
    public FileDownloadResponseDto getFile(final Long fileId) {
        // 1) null 검사
        if (fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 파일 정보 조회
        final File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));

        // 3) 단순 정보 추출
        final String name = file.getName();
        final String mimeType = file.getMimeType();
        final Long size = file.getSize();
        final String relativePath = file.getRelativePath();

        // 4) 파일 읽어오기
        final Resource resource = storageManager.getFile(Path.of(relativePath));

        // 5) DTO 조립
        return new FileDownloadResponseDto(
                name,
                mimeType,
                size,
                resource
        );
    }

    /**
     * 실시간 트랜스코딩으로 파일을 스트리밍하는 메서드
     * @param fileId 스트리밍할 파일 ID
     * @param start 영상 스트리밍 시작 지점
     * @return 트랜스코딩된 파일 정보
     */
    public StreamingResponseBody streamFile(
            final Long fileId,
            final double start
    ) {
        // 1) null 검사
        if (fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 파일 위치 조회 - 캐시에 먼저 조회하고 없으면 DB 조회
        final String path = filePathCache.get(
                fileId, id -> fileRepository.findById(fileId)
                    .map(File::getRelativePath)
                    .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST))
        );

        // 3) 절대 경로로 변환
        final Path absPath = storageManager.resolvePath(path, false);

        // 4) 실시간 트랜스코딩
        return ffmpegProcessor.getVideoStream(absPath.toString(), start);

    }

    /**
     * 요청 받은 파일을 휴지통으로 보내는 메서드
     * @param fileId 휴지통으로 보낼 메서드
     */
    @Transactional
    public void deleteFile(final Long fileId) {
        // 1) null 검사
        if (fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 파일 조회
        final File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));

        // 3) DB에서 파일 삭제
        fileRepository.delete(file);

        // 4) 실제 파일 삭제
        storageManager.removeFile(file.getRelativePath());
    }

}
