package com.seohamin.fshs.v2.domain.user.dto;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.user.entity.User;

import java.time.Instant;
import java.util.Optional;

public record UserResponseDto(
    Long id,
    String username,
    String role,
    Long rootFolderId,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserResponseDto of(
            final User user
    ) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getUserRole().name(),
                Optional.ofNullable(user.getRootFolder())
                        .map(Folder::getId)
                        .orElse(null),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
