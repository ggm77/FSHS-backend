package com.seohamin.fshs.v2.domain.folder.dto;

import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.user.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record FolderResponseDto(
        Long id,
        Long ownerId,
        Long parentFolderId,
        String relativePath,
        String name,
        Instant originUpdatedAt,
        Instant createdAt,
        Instant updatedAt,
        Boolean isRoot,
        List<SimpleFolderResponseDto> folders,
        List<FileResponseDto> files
) {

    public static FolderResponseDto of(final Folder folder) {
        return new FolderResponseDto(
                folder.getId(),
                Optional.ofNullable(folder.getOwner())
                        .map(User::getId)
                        .orElse(null),
                folder.getParentFolder().getId(),
                folder.getRelativePath(),
                folder.getName(),
                folder.getOriginUpdatedAt(),
                folder.getCreatedAt(),
                folder.getUpdatedAt(),
                folder.getIsRoot(),
                folder.getFolders().stream()
                        .map(SimpleFolderResponseDto::of)
                        .filter(dto -> !dto.id().equals(1L))
                        .toList(),
                folder.getFiles().stream().map(FileResponseDto::of).toList()
        );
    }
}
