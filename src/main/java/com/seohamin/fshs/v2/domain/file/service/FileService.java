package com.seohamin.fshs.v2.domain.file.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.file.dto.*;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // HLS 세그먼트 파일명 패턴 (예: segment12.ts) — 자릿수를 제한해 int 오버플로 방지
    private static final Pattern HLS_SEGMENT_PATTERN = Pattern.compile("segment(\\d{1,9})\\.ts");

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

    public InputStreamResource streamHlsFile(
            final Long fileId,
            final String hlsFile,
            final String username
    ) {

        // 1) null 검사
        if (fileId == null || hlsFile == null || username == null) {
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

        // 5) 재생목록(.m3u8) 요청이면 DB duration으로 즉시 생성해 반환 (ffprobe 불필요)
        if (hlsFile.endsWith(".m3u8")) {
            final File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));
            return ffmpegProcessor.getHlsPlaylist(file.getDuration());
        }

        // 6) 세그먼트(.ts) 요청이면 인덱스를 파싱해 해당 구간만 실시간 트랜스코딩
        final Matcher matcher = HLS_SEGMENT_PATTERN.matcher(hlsFile);
        if (!matcher.matches()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        return ffmpegProcessor.getHlsSegment(absPath, Integer.parseInt(matcher.group(1)));
    }

    /**
     * 파일 정보 수정하는 메서드
     * 파일 옮기기 포함
     * @param fileId 수정할 파일 아이디
     * @param fileUpdateRequestDto 수정할 정보
     * @param username 요청 유저명
     * @return 수정된 파일의 정보
     */
    @Transactional
    public FileResponseDto updateFile(
            final Long fileId,
            final FileUpdateRequestDto fileUpdateRequestDto,
            final String username
    ) {

        // 1) null 검사
        if (fileId == null || fileUpdateRequestDto == null || username == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 파일 옮기는지 확인
        boolean moveFile = fileUpdateRequestDto.folderId() != null;

        // 3) 유저 및 파일 조회 및 권한 체크
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
        final File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));

        // 파일 권한 체크
        if (!hasPermission(user, file)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 4) 파일 이동 한다면 폴더 조회 및 검증, 이동
        final Folder folder;
        if (moveFile) {
            folder = folderRepository.findById(fileUpdateRequestDto.folderId())
                    .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

            // 폴더 권한 체크
            if (!hasPermission(user, folder)) {
                throw new CustomException(ExceptionCode.INVALID_PATH);
            }

            // 파일 이동
            moveFile(file, folder);
        }

        // 5) 마지막 수정시각 정보 수정
        if (fileUpdateRequestDto.originUpdatedAt() != null) {
            file.updateOriginUpdatedAt(fileUpdateRequestDto.originUpdatedAt());
        }

        return FileResponseDto.of(file);
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

        // 5) DB에서 파일 삭제 후 flush — DB 삭제를 디스크보다 먼저 확정한다.
        //    DB 측 실패가 나면 디스크를 건드리지 않는다 (이동과 동일한 순서).
        fileRepository.delete(file);
        fileRepository.flush();

        // 6) DB 삭제가 끝난 뒤 마지막으로 실제 파일 삭제.
        //    디스크 삭제가 실패하면 예외로 트랜잭션이 롤백돼 DB 삭제도 원복된다.
        storageManager.removeFile(file.getRelativePath());

        // 7) 삭제된 파일 관련 캐시 무효화
        filePathCache.invalidate(fileId);
        fileStatusCache.invalidate(file.getUuid());
        fileAccessCache.asMap().keySet().removeIf(key -> key.startsWith(fileId + ":"));
    }

    /**
     * 특정 폴더로 파일을 이동시키는 메서드
     * @param file 이동시킬 파일
     * @param folder 목적지 폴더
     */
    private void moveFile(
            final File file,
            final Folder folder
    ) {

        // 1) null 검사
        if (file == null || folder == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 폴더 경로 추출
        final Path filePath = Path.of(file.getRelativePath());
        final Path folderPath = Path.of(folder.getRelativePath());

        // 3) 부모 폴더 재지정 (소유측 FK 만 변경)
        //    Folder.files 는 orphanRemoval=true 라 inverse 컬렉션에서 remove 하면 파일이 삭제된다.
        //    폴더 이동과 동일하게 컬렉션은 건드리지 않고 FK 만 바꾼다.
        file.updateParentFolder(folder);

        // 4) DB에서 경로 수정 (파일 경로 + 부모 경로)
        file.updateRelativePath(folderPath.resolve(file.getName()).toString());
        file.updateParentPath(folderPath.toString());

        // 5) 디스크 이동 전에 flush 해 이름 충돌(uk_file_path) 등 DB 실패를 먼저 드러낸다.
        //    실패하면 디스크를 건드리기 전에 예외가 나므로 디스크-DB 정합성이 유지된다.
        try {
            fileRepository.flush();
        } catch (final DataIntegrityViolationException e) {
            // 목적지에 같은 이름의 파일이 이미 존재 (uk_file_path 위반) → 400
            throw new CustomException(ExceptionCode.FILE_ALREADY_EXIST, e);
        }

        // 6) DB 작업이 끝난 뒤 마지막으로 디스크 이동.
        //    디스크 이동이 실패하면 예외로 트랜잭션이 롤백돼 DB 도 원복된다.
        storageManager.moveFile(filePath, folderPath);

        // 7) 경로/권한 캐시 무효화 — 이동 후 옛 경로가 서빙되는 것을 방지
        filePathCache.invalidate(file.getId());
        fileAccessCache.asMap().keySet().removeIf(key -> key.startsWith(file.getId() + ":"));
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
