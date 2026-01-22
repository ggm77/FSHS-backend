package org.iptime.raspinas.FSHS.v2.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import org.iptime.raspinas.FSHS.v2.domain.user.entity.UserV2;

@Getter
public class UserResponseDtoV2 {
    private final String username;
    private final String role;
    private final Long rootFolderId;

    @Builder
    public UserResponseDtoV2(
            final UserV2 user,
            final Long rootFolderId
    ) {
        this.username = user.getUsername();
        this.role = user.getRole().name();
        this.rootFolderId = rootFolderId;
    }
}
