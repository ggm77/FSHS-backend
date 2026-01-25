package com.seohamin.fshs.v2.domain.auth.login.dto;

import lombok.Builder;
import lombok.Getter;
import com.seohamin.fshs.v2.domain.user.entity.Role;

@Getter
public class LoginResponseDto {

    private final String role;
    private final String accessToken;
    private final Long expTime;
    private final String refreshToken;

    @Builder
    public LoginResponseDto(
            final Role role,
            final String accessToken,
            final Long expTime,
            final String refreshToken
    ) {
        this.role = role.name();
        this.accessToken = accessToken;
        this.expTime = expTime;
        this.refreshToken = refreshToken;
    }
}
