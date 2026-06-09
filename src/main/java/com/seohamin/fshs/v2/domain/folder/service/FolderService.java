package com.seohamin.fshs.v2.domain.folder.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
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
    private final FileRepository fileRepository;
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
                .name(name)
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
     * 폴더를 이동하거나 이름을 변경하는 메서드
     * 이동과 이름 변경은 디스크 상에선 모두 "최종 경로로 옮기기"라 한 번의 이동으로 처리하고,
     * 하위 폴더/파일의 상대 경로는 DB에서 일괄 재작성한다
     * @param folderId 폴더 id
     * @param folderRequestDto 목적지 부모 폴더 id, 새 이름이 담긴 DTO
     * @param username 요청한 유저 명
     * @return 수정된 폴더 정보
     */
    @Transactional
    public FolderResponseDto updateFolder(
            final Long folderId,
            final FolderRequestDto folderRequestDto,
            final String username
    ) {
        // 1) null 검사
        if (folderId == null || folderRequestDto == null || username == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 유저/폴더 조회 및 접근 권한 확인
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
        final Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));
        if (!hasPermission(user, folder)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        // 3) 시스템 루트는 이동/이름 변경 불가
        if (folder.getIsSystemRoot()) {
            throw new CustomException(ExceptionCode.RESTRICT_MOVE_SYSTEM_ROOT);
        }

        // 4) 목적지 부모 폴더 결정 (parentFolderId 없으면 현재 부모 유지)
        final Folder oldParent = folder.getParentFolder();
        final Folder newParent;
        if (folderRequestDto.parentFolderId() != null) {
            newParent = folderRepository.findById(folderRequestDto.parentFolderId())
                    .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

            // 목적지 접근 권한 확인
            if (!hasPermission(user, newParent)) {
                throw new CustomException(ExceptionCode.INVALID_PATH);
            }

            // 자기 자신이나 하위 폴더로는 이동 불가 (순환 방지)
            final Path folderPath = Path.of(folder.getRelativePath()).normalize();
            final Path newParentPath = Path.of(newParent.getRelativePath()).normalize();
            if (newParentPath.startsWith(folderPath)) {
                throw new CustomException(ExceptionCode.RESTRICT_MOVE_INTO_DESCENDANT);
            }
        } else {
            newParent = oldParent;
        }

        // 5) 새 이름 결정 (name 없으면 현재 이름 유지)
        final String newName = (folderRequestDto.name() != null && !folderRequestDto.name().isBlank())
                ? PathNameUtil.normalize(folderRequestDto.name())
                : folder.getName();

        // 6) 실제 변경 사항 없으면 그대로 반환
        final boolean parentChanged = !newParent.getId().equals(oldParent.getId());
        final boolean nameChanged = !newName.equals(folder.getName());
        if (!parentChanged && !nameChanged) {
            return FolderResponseDto.of(folder);
        }

        // 7) 옛/새 경로 확보 (하위 치환에 쓸 옛 경로는 변경 전에 확보)
        final Path oldPath = Path.of(folder.getRelativePath());
        final Path newPath = Path.of(newParent.getRelativePath()).resolve(newName);
        final String oldPathStr = folder.getRelativePath();
        final String newPathStr = newPath.toString();

        // 8) 부모 폴더 재지정
        //    소유측 FK(parentFolder)만 변경한다. folders 컬렉션은 orphanRemoval=true 이므로
        //    기존 부모 컬렉션에서 remove 하면 자식 있는 폴더가 통째로 삭제(cascade)되어 절대 건드리지 않는다.
        if (parentChanged) {
            folder.updateParentFolder(newParent);
        }

        // 9) 폴더 자신의 이름/경로 갱신
        folder.updateName(newName);
        folder.updateRelativePath(newPathStr);

        // 10) 하위 폴더/파일의 경로 접두사를 LIKE 치환으로 일괄 갱신 (재귀 N+1 제거)
        //     벌크 연산의 flushAutomatically 가 폴더 자신의 변경까지 DB 에 반영하므로
        //     이름 충돌(uk_folder_path) 등 DB 실패는 디스크를 건드리기 전에 드러난다
        final String pattern = escapeLike(oldPathStr) + "/%";
        final int cutFrom = oldPathStr.length() + 1;
        try {
            folderRepository.rewriteDescendantPaths(newPathStr, cutFrom, pattern);
            fileRepository.rewriteDescendantPaths(newPathStr, cutFrom, pattern);
        } catch (final DataIntegrityViolationException e) {
            // 목적지에 같은 이름의 폴더가 이미 존재 (uk_folder_path 위반) → 400
            throw new CustomException(ExceptionCode.FILE_ALREADY_EXIST, e);
        }

        // 11) DB 작업이 모두 끝난 뒤 마지막으로 디스크 이동.
        //     디스크 이동이 실패하면 예외로 트랜잭션이 롤백돼 DB 도 원복되므로 정합성이 유지된다.
        storageManager.moveFolder(oldPath, newPath);

        // 12) 경로/접근 캐시 무효화 (하위 파일 경로가 일괄 변경됨)
        filePathCache.invalidateAll();
        fileAccessCache.invalidateAll();

        // 13) clear 로 detached 된 폴더를 다시 조회해 응답 생성
        final Folder updated = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));
        return FolderResponseDto.of(updated);
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
     * LIKE 패턴에서 와일드카드로 해석되는 문자(\ % _)를 이스케이프한다 (ESCAPE '\' 기준)
     * 폴더명에 _ 나 % 가 들어가도 형제 경로가 잘못 치환되지 않도록 한다
     * @param raw 접두사로 쓸 원본 경로 문자열
     * @return 이스케이프된 문자열
     */
    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
