package com.seohamin.fshs.v2.domain.file.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileStatusResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileUploadResponseDto;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.entity.Status;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final StorageManager storageManager;
    private final FileUploadProcessor fileUploadProcessor;
    private final Cache<Long, String> filePathCache;
    private final Cache<String, Status> fileStatusCache;
    private final Cache<String, Boolean> fileAccessCache;
    private final FfmpegProcessor ffmpegProcessor;

    /**
     * 파일 하나 업로드처리하는 메서드
     *
     * @param multipartFile 업로드할 파일
     * @param lastModified 업로드할 파일의 마지막 수정 시점
     * @param folderId 저장할 상위 폴더 ID
     * @param username 업로드 요청 유저명
     * @return 업로드된 파일의 정보
     */
    public FileUploadResponseDto uploadFile(
            final MultipartFile multipartFile,
            final Instant lastModified,
            final Long folderId,
            final String username
    ) {

        // 1) null 검사
        if (multipartFile == null || lastModified == null || folderId == null || username == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 3) 상위 폴더 조회
        final Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 4) 업로드 폴더가 해당 유저의 루트 폴더 하위인지 판단
        if (!hasPermission(user, folder)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 5) 임시 폴더에 파일 저장 - MultipartFile 수명 문제 때문에 요청 스레드에서 동기로 처리
        final Path tempFilePath = storageManager.saveTemporarily(multipartFile);

        // 6) 파일 UUID 발급 및 처리 중 상태 등록
        final String fileUuid = UUID.randomUUID().toString();
        fileStatusCache.put(fileUuid, Status.PROCESSING);

        // 7) 비동기 처리 위임
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
     * @param username 요청 유저명
     * @return 파일 정보 담긴 DTO
     */
    public FileResponseDto getFileDetails(
            final Long fileId,
            final String username
    ) {
        // 1) null 검사
        if(fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 정보 조회
        final File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));

        // 3) 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 접근 권한 확인
        if (!hasPermission(user, file)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 5) DTO에 담기
        return FileResponseDto.of(file);
    }


    /**
     * 파일을 다운로드 하거나 직접 스트리밍을 해주는 메서드
     * @param fileId 프론트로 전송할 파일의 ID
     * @param username 요청 유저명
     * @return 파일의 정보가 담긴 DTO
     */
    public FileDownloadResponseDto getFile(
            final Long fileId,
            final String username
    ) {
        // 1) null 검사
        if (fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 파일 정보 조회
        final File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));

        // 3) 접근 권한 조회 - 캐시 히트 시 유저 DB 조회 생략 (range 요청 반복 최적화)
        final boolean canAccess = fileAccessCache.get(
                fileId + ":" + username,
                k -> {
                    final User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
                    return hasPermission(user, file);
                }
        );
        if (!canAccess) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 4) 파일 읽어오기
        final Resource resource = storageManager.getFile(Path.of(file.getRelativePath()));

        // 5) DTO 조립
        return new FileDownloadResponseDto(
                file.getName(),
                file.getMimeType(),
                file.getSize(),
                resource
        );
    }

    /**
     * 실시간 트랜스코딩으로 파일을 스트리밍하는 메서드
     * @param fileId 스트리밍할 파일 ID
     * @param start 영상 스트리밍 시작 지점
     * @param username 요청 유저명
     * @return 트랜스코딩된 스트리밍 바디
     */
    public StreamingResponseBody streamFile(
            final Long fileId,
            final double start,
            final String username
    ) {
        // 1) null 검사
        if (fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 접근 권한 조회 - 캐시 히트 시 DB 조회 없음 (반복 세그먼트 요청 최적화)
        //    캐시 미스 시 파일+유저 DB 조회 후 filePathCache도 함께 채움
        final boolean canAccess = fileAccessCache.get(
                fileId + ":" + username,
                k -> {
                    final File file = fileRepository.findById(fileId)
                            .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));
                    final User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
                    filePathCache.put(fileId, file.getRelativePath());
                    return hasPermission(user, file);
                }
        );
        if (!canAccess) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 3) 파일 경로 조회 - 캐시에 먼저 조회하고 없으면 DB 조회
        final String path = filePathCache.get(
                fileId, id -> fileRepository.findById(fileId)
                    .map(File::getRelativePath)
                    .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST))
        );

        // 4) 절대 경로로 변환
        final Path absPath = storageManager.resolvePath(path, false);

        // 5) 실시간 트랜스코딩
        return ffmpegProcessor.getVideoStream(absPath.toString(), start);
    }

    /**
     * 요청 받은 파일을 휴지통으로 보내는 메서드
     * @param fileId 휴지통으로 보낼 파일 ID
     * @param username 요청 유저명
     */
    @Transactional
    public void deleteFile(
            final Long fileId,
            final String username
    ) {
        // 1) null 검사
        if (fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 파일 조회
        final File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));

        // 3) 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 접근 권환 조회
        if (!hasPermission(user, file)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 5) DB에서 파일 삭제
        fileRepository.delete(file);

        // 6) 실제 파일 삭제
        storageManager.removeFile(file.getRelativePath());
    }

    /**
     * 해당 유저가 폴더에 접근 권한을 가졌는지 확인하는 메서드
     * @param user 접근 권한 확인할 유저
     * @param folder 접근 권한 확인할 폴더
     * @return 접근 가능 여부
     */
    private boolean hasPermission(final User user, final Folder folder) {
        final Folder userRootFolder = user.getRootFolder();
        if (userRootFolder == null) {
            throw new CustomException(ExceptionCode.ROOT_NOT_EXIST);
        }
        final String userRootPath = userRootFolder.getRelativePath();
        if (userRootPath.isEmpty()) return true; // 유저 루트 폴더가 시스템 루트 폴더일 때 예외 처리
        final Path userRootFolderPath = Path.of(userRootPath).normalize();
        final Path folderPath = Path.of(folder.getRelativePath()).normalize();
        return folderPath.startsWith(userRootFolderPath);
    }

    /**
     * 해당 유저가 파일에 접근 권한을 가졌는지 확인하는 메서드
     * @param user 접근 권한 확인할 유저
     * @param file 접근 권한 확인할 파일
     * @return 접근 가능 여부
     */
    private boolean hasPermission(final User user, final File file) {
        final Folder userRootFolder = user.getRootFolder();
        if (userRootFolder == null) {
            throw new CustomException(ExceptionCode.ROOT_NOT_EXIST);
        }
        final String userRootPath = userRootFolder.getRelativePath();
        if (userRootPath.isEmpty()) return true; // 유저 루트 폴더가 시스템 루트 폴더일 때 예외 처리
        final Path userRootFolderPath = Path.of(userRootPath).normalize();
        final Path folderPath = Path.of(file.getRelativePath()).normalize();
        return folderPath.startsWith(userRootFolderPath);
    }

}
