package com.seohamin.fshs.v2.domain.folder.dto;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.user.entity.User;

import java.time.Instant;
import java.util.Optional;

public record FolderResponseDto(
        Long id,
        Long ownerId,
        Long parentFolderId,
        String relativePath,
        String name,
        Instant originCreatedAt,
        Instant originUpdatedAt,
        Instant createdAt,
        Instant updatedAt,
        Boolean isRoot
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
                folder.getOriginCreatedAt(),
                folder.getOriginUpdatedAt(),
                folder.getCreatedAt(),
                folder.getUpdatedAt(),
                folder.getIsRoot()
        );
    }
}
