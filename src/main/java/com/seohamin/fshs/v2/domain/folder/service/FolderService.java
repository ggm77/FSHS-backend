package com.seohamin.fshs.v2.domain.folder.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.folder.dto.FolderDownloadResponseDto;
import com.seohamin.fshs.v2.domain.folder.dto.FolderRequestDto;
import com.seohamin.fshs.v2.domain.folder.dto.FolderResponseDto;
import com.seohamin.fshs.v2.domain.folder.dto.SimpleFolderResponseDto;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import com.seohamin.fshs.v2.global.util.storage.PathNameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageManager storageManager;
    private final Cache<Long, String> filePathCache;
    private final Cache<String, Boolean> fileAccessCache;

    /**
     * 폴더 생성하는 메서드
     * 폴더 생성 시점과 마지막 수정 시점은 현재 시간으로 함
     * @param folderRequestDto 폴더 정보 담긴 DTO
     * @param username 요청 유저명
     * @return 생성된 폴더의 정보 DTO
     */
    @Transactional
    public FolderResponseDto createFolder(
            final FolderRequestDto folderRequestDto,
            final String username
    ) {
        // 1) 변수에 저장
        final Long parentFolderId = folderRequestDto.parentFolderId();
        final String rawName = folderRequestDto.name();

        // 2) null 검사
        if (parentFolderId == null || rawName == null || rawName.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 3) 유저 정보 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 유저 루트 폴더 존재 확인
        final Folder userRootFolder = user.getRootFolder();
        if (userRootFolder == null) {
            throw new CustomException(ExceptionCode.ROOT_NOT_EXIST);
        }

        // 5) 폴더명 정규화
        final String name = PathNameUtil.normalize(rawName);

        // 6) 상위 폴더 정보 가져오기
        final Folder parentFolder = folderRepository.findById(parentFolderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 7) 상위 폴더가 유저 루트 폴더 하위인지 검증
        final String userRootPath = userRootFolder.getRelativePath();
        if (!userRootPath.isEmpty()) { // 유저 루트 폴더가 시스템 루트 폴더일 때 예외 처리
            final Path userRootFolderPath = Path.of(userRootPath).normalize();
            final Path parentFolderPath = Path.of(parentFolder.getRelativePath()).normalize();
            if (!parentFolderPath.startsWith(userRootFolderPath)) {
                throw new CustomException(ExceptionCode.STORAGE_ACCESS_DENIED);
            }
        }

        // 8) 상대 경로 생성
        final Path targetPath = Path.of(parentFolder.getRelativePath()).resolve(name);

        // 9) 폴더 생성
        final Path createdFolderPath = storageManager.createFolder(targetPath);

        // 10) 폴더 엔티티 생성
        final Folder folder = Folder.builder()
                .parentFolder(parentFolder)
                .ownerId(user.getId())
                .relativePath(createdFolderPath.toString())
                .name(name.toLowerCase())
                .originUpdatedAt(Instant.now())
                .isRoot(false)
                .build();

        // 11) DB에 저장
        final Folder savedFolder = folderRepository.save(folder);

        return FolderResponseDto.of(savedFolder);
    }

    /**
     * 폴더 정보 조회하는 메서드
     * @param folderId 조회할 폴더 아이디
     * @param username 요청 유저명
     * @return 폴더 정보 담긴 DTO
     */
    public FolderResponseDto getFolder(
            final Long folderId,
            final String username
    ) {
        // 1) null 검사
        if (folderId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 폴더 조회
        final Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 3) 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 업로드 폴더가 해당 유저의 루트 폴더 하위인지 판단
        if (!hasPermission(user, folder)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        return FolderResponseDto.of(folder);
    }

    /**
     * 폴더 전체를 ZIP으로 압축해서 스트리밍하는 메서드
     * @param folderId 다운로드할 폴더 아이디
     * @param username 요청 유저명
     * @return ZIP 스트리밍 바디
     */
    public FolderDownloadResponseDto downloadFolder(
            final Long folderId,
            final String username
    ) {
        // 1) null 검사
        if (folderId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 폴더 조회
        final Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 3) 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 업로드 폴더가 해당 유저의 루트 폴더 하위인지 판단
        if (!hasPermission(user, folder)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 5) 폴더 절대경로로 변환 및 폴더명 추출
        final Path folderAbsPath = storageManager.resolvePath(folder.getRelativePath(), false);
        final String folderName = folder.getName();

        // 6) 총 비압축 크기 계산 (프론트 다운로드 진행률용)
        final long totalSize;
        try (Stream<Path> sizeWalk = Files.walk(folderAbsPath)) {
            totalSize = sizeWalk
                    .filter(p -> !Files.isDirectory(p))
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0L; }
                    })
                    .sum();
        } catch (IOException e) {
            throw new CustomException(ExceptionCode.FILE_READ_ERROR, e);
        }

        // 7) ZIP 스트리밍 바디 생성
        final StreamingResponseBody stream = outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream);
                 Stream<Path> paths = Files.walk(folderAbsPath)) {
                for (final Path p : (Iterable<Path>) paths::iterator) {
                    if (p.equals(folderAbsPath)) continue;
                    final String entryName = folderName + "/" + folderAbsPath.relativize(p);
                    if (Files.isDirectory(p)) {
                        zos.putNextEntry(new ZipEntry(entryName + "/"));
                        zos.closeEntry();
                        continue;
                    }
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(p, zos);
                    zos.closeEntry();
                }
            }
        };

        return new FolderDownloadResponseDto(folderName, totalSize, stream);
    }

    /**
     * 폴더 삭제하는 메서드
     * @param folderId 삭제할 폴더 id
     * @param username 유저 정보
     */
    @Transactional
    public void deleteFolder(
            final Long folderId,
            final String username
    ) {
        // 1) null 검사
        if (folderId == null || username == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 3) 폴더 조회
        final Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 4) 시스템 루트 보호
        if (folder.getIsSystemRoot()) {
            throw new CustomException(ExceptionCode.RESTRICT_DELETE_SYSTEM_ROOT);
        }

        // 5) 접근 권한 조회
        if (!hasPermission(user, folder)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 6) DB에서 삭제
        folderRepository.delete(folder);

        // 7) 디스크에서 삭제
        storageManager.removeFolder(folder.getRelativePath());

        // 8) 하위 파일 관련 캐시 무효화 (폴더 삭제는 드물어 전체 무효화로 단순 처리)
        filePathCache.invalidateAll();
        fileAccessCache.invalidateAll();
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
}
