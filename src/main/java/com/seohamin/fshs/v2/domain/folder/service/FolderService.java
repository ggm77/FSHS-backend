package com.seohamin.fshs.v2.domain.folder.service;

import com.seohamin.fshs.v2.domain.folder.dto.FolderRequestDto;
import com.seohamin.fshs.v2.domain.folder.dto.FolderResponseDto;
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

import java.nio.file.Path;
import java.time.Instant;

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

        // 3) 폴더명 정규화
        final String name = PathNameUtil.normalize(rawName);

        // 4) 상위 폴더 정보 가져오기
        final Folder parentFolder = folderRepository.findById(parentFolderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 5) 유저 정보 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 5) 상대 경로 생성
        final Path targetPath;
        // 시스템 루트가 상위 폴더인 경우
        if (parentFolder.getIsSystemRoot()) {
            targetPath = Path.of(name);
        } else {
            targetPath = Path.of(parentFolder.getRelativePath()).resolve(name);
        }

        // 6) 폴더 생성
        final Path createdFolderPath = storageManager.createFolder(targetPath);

        // 7) 폴더 엔티티 생성
        final Folder folder = Folder.builder()
                .parentFolder(parentFolder)
                .ownerId(user.getId())
                .relativePath(createdFolderPath.toString())
                .name(name.toLowerCase())
                .originUpdatedAt(Instant.now())
                .isRoot(isRoot)
                .build();

        // 8) DB에 저장
        final Folder savedFolder = folderRepository.save(folder);

        return FolderResponseDto.of(savedFolder);
    }
}
