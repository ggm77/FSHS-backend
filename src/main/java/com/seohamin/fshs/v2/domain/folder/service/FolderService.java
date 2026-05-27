package com.seohamin.fshs.v2.domain.folder.service;

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
import com.seohamin.fshs.v2.global.init.SystemRootInitializer;
import com.seohamin.fshs.v2.global.util.storage.PathNameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

    /**
     * 폴더 생성하는 메서드
     * 폴더 생성 시점과 마지막 수정 시점은 현재 시간으로 함
     * @param folderRequestDto 폴더 정보 담긴 DTO
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
        final Boolean isRoot = folderRequestDto.isRoot();

        // 2) null 검사
        if (
                parentFolderId == null || isRoot == null
                || rawName == null ||  rawName.isBlank()
        ) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 3) 루트 폴더 검증
        if (isRoot && folderRepository.existsByIsRoot(true)) {
            throw new CustomException(ExceptionCode.ROOT_ALREADY_EXIST);
        }

        // 4) 폴더명 정규화
        final String name = PathNameUtil.normalize(rawName);

        // 5) 상위 폴더 정보 가져오기
        final Folder parentFolder = folderRepository.findById(parentFolderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 6) 유저 정보 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 7) 상대 경로 생성
        final Path targetPath;
        // 시스템 루트가 상위 폴더인 경우
        if (parentFolder.getIsSystemRoot()) {
            targetPath = Path.of(name);
        } else {
            targetPath = Path.of(parentFolder.getRelativePath()).resolve(name);
        }

        // 8) 폴더 생성
        final Path createdFolderPath = storageManager.createFolder(targetPath);

        // 9) 폴더 엔티티 생성
        final Folder folder = Folder.builder()
                .parentFolder(parentFolder)
                .ownerId(user.getId())
                .relativePath(createdFolderPath.toString())
                .name(name.toLowerCase())
                .originUpdatedAt(Instant.now())
                .isRoot(isRoot)
                .build();

        // 10) DB에 저장
        final Folder savedFolder = folderRepository.save(folder);

        return FolderResponseDto.of(savedFolder);
    }

    /**
     * 폴더 정보 조회하는 메서드
     * @param folderId 조회할 폴더 아이디
     * @return 폴더 정보 담긴 DTO
     */
    public FolderResponseDto getFolder(final Long folderId) {
        // 1) null 검사
        if (folderId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 폴더 조회
        final Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 3) 시스템 루트 차단
        if (folder.getIsSystemRoot()) {
            throw new CustomException(ExceptionCode.SYSTEM_ROOT_FORBIDDEN);
        }

        return FolderResponseDto.of(folder);
    }

    /**
     * 폴더 전체를 ZIP으로 압축해서 스트리밍하는 메서드
     * @param folderId 다운로드할 폴더 아이디
     * @return ZIP 스트리밍 바디
     */
    public FolderDownloadResponseDto downloadFolder(final Long folderId) {
        if (folderId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        if (folder.getIsSystemRoot()) {
            throw new CustomException(ExceptionCode.SYSTEM_ROOT_FORBIDDEN);
        }

        final Path folderAbsPath = storageManager.resolvePath(folder.getRelativePath(), false);
        final String folderName = folder.getName();

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

        return new FolderDownloadResponseDto(folderName, stream);
    }
}
