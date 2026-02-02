package com.seohamin.fshs.v2.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import com.seohamin.fshs.v2.domain.user.entity.User;

import java.time.Instant;

@Getter
public class UserResponseDto {
    private final Long id;
    private final String username;
    private final String role;
    private final Long rootFolderId;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    public UserResponseDto(
            final User user,
            final Long rootFolderId
    ) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.role = user.getRole().name();
        this.rootFolderId = rootFolderId;
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
