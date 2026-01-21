package org.iptime.raspinas.FSHS.v1.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.v1.domain.auth.dto.RefreshTokenRequestDto;
import org.iptime.raspinas.FSHS.v1.domain.auth.dto.RefreshTokenResponseDto;
import org.iptime.raspinas.FSHS.v1.global.auth.jwt.TokenProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final TokenProvider tokenProvider;

    public RefreshTokenResponseDto refreshToken(
            final RefreshTokenRequestDto refreshTokenRequestDto
    ){

        final String stringUserId = tokenProvider.validate(refreshTokenRequestDto.getRefreshToken());
        final Long userId = Long.parseLong(stringUserId);

        final String accessToken = tokenProvider.createAccessToken(userId);
        final String refreshToken = tokenProvider.createRefreshToken(userId);

        final RefreshTokenResponseDto refreshTokenResponseDto = RefreshTokenResponseDto
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return refreshTokenResponseDto;
    }
}
