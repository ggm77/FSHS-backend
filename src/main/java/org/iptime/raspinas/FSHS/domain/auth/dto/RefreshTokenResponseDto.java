package org.iptime.raspinas.FSHS.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponseDto {
    private String accessToken;
    private String refreshToken;
}
