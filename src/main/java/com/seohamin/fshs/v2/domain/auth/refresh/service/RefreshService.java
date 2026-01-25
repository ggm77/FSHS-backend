package com.seohamin.fshs.v2.domain.auth.refresh.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.auth.login.dto.LoginResponseDto;
import com.seohamin.fshs.v2.domain.auth.refresh.dto.RefreshRequestDto;
import com.seohamin.fshs.v2.domain.user.entity.Role;
import com.seohamin.fshs.v2.global.auth.jwt.JwtProvider;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.util.EnumUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshService {

    private final JwtProvider jwtProvider;
    private final EnumUtil enumUtil;

    /**
     * 리프레시 토큰 받아서 JWT 새로 발급하는 메서드
     * @param refreshRequestDto 리프레시 토큰 담긴 DTO
     * @return 새로운 JWT
     */
    public LoginResponseDto refresh(final RefreshRequestDto refreshRequestDto) {

        // 1) 변수에 지정
        final String oldRefreshToken = refreshRequestDto.getRefreshToken();

        // 2) Claims 추출
        final Claims claims = jwtProvider.getClaims(oldRefreshToken);

        // 3) 유저 ID와 Role 추출
        final Long userId = Long.valueOf(claims.getSubject());
        final String roleKey = jwtProvider.getAuthorities(claims).get(0).getAuthority();
        final Role role = enumUtil.toEnum(Role.class, roleKey.substring("ROLE_".length()))
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_ENUM_VALUE));

        // 4) JWT 발급
        final String accessToken = jwtProvider.createAccessToken(userId, role);
        final Long expTime = jwtProvider.getAccessTokenExpirationSeconds();
        final String refreshToken = jwtProvider.createRefreshToken(userId);

        // 5) DTO에 담아서 리턴
        return LoginResponseDto.builder()
                .role(role)
                .accessToken(accessToken)
                .expTime(expTime)
                .refreshToken(refreshToken)
                .build();
    }
}
