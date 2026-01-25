package com.seohamin.fshs.v2.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import com.seohamin.fshs.v2.domain.user.entity.User;

@Getter
public class UserResponseDto {
    private final String username;
    private final String role;
    private final Long rootFolderId;

    @Builder
    public UserResponseDto(
            final User user,
            final Long rootFolderId
    ) {
        this.username = user.getUsername();
        this.role = user.getRole().name();
        this.rootFolderId = rootFolderId;
    }
}
