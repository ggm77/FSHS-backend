package com.seohamin.fshs.v2.domain.folder.dto;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.user.entity.User;

import java.time.Instant;
import java.util.Optional;

public record SimpleFolderResponseDto(
        Long id,
        Long ownerId,
        Long parentFolderId,
        String relativePath,
        String name,
        Instant originUpdatedAt,
        Instant createdAt,
        Instant updatedAt,
        Boolean isRoot
) {

    public static SimpleFolderResponseDto of(final Folder folder) {
        return new SimpleFolderResponseDto(
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
                folder.getIsRoot()
        );
    }
}
