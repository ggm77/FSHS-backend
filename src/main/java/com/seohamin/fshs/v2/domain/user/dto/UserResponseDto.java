package com.seohamin.fshs.v2.domain.user.dto;

import com.seohamin.fshs.v2.domain.user.entity.User;

import java.time.Instant;

public record UserResponseDto(
    Long id,
    String username,
    String role,
    Long rootFolderId,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserResponseDto of(
            final User user,
            final Long rootFolderId
    ) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getUserRole().name(),
                rootFolderId,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
