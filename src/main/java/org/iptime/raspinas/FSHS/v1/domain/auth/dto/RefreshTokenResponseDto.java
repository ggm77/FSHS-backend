package org.iptime.raspinas.FSHS.v1.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponseDto {
    private String accessToken;
    private String refreshToken;
}
