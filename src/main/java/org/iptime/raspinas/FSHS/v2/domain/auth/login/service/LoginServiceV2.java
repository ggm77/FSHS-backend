package org.iptime.raspinas.FSHS.v2.domain.auth.login.service;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v2.domain.auth.login.dto.LoginRequestDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.auth.login.dto.LoginResponseDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.entity.Role;
import org.iptime.raspinas.FSHS.v2.domain.user.entity.UserV2;
import org.iptime.raspinas.FSHS.v2.domain.user.repository.UserRepositoryV2;
import org.iptime.raspinas.FSHS.v2.global.auth.jwt.JwtProvider;
import org.iptime.raspinas.FSHS.v2.global.config.SecurityConfigV2;
import org.iptime.raspinas.FSHS.v2.global.exception.CustomExceptionV2;
import org.iptime.raspinas.FSHS.v2.global.exception.constants.ExceptionCodeV2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginServiceV2 {

    private final JwtProvider jwtProvider;
    private final SecurityConfigV2 securityConfig;
    private final UserRepositoryV2 userRepository;

    /**
     * 로그인 진행해서 JWT 발급하는 메서드
     * @param loginRequestDto 로그인 정보 담긴 DTO
     * @return JWT
     */
    public LoginResponseDtoV2 login(final LoginRequestDtoV2 loginRequestDto) {

        // 1) 유저명 변수에 지정
        final String username = loginRequestDto.getUsername();

        // 2) 유저명으로 유저 엔티티 조회
        final UserV2 user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomExceptionV2(ExceptionCodeV2.USER_NOT_EXIST));

        // 3) 비밀번호 비교
        final boolean isPasswordMatch = securityConfig.passwordEncoderV2().matches(
                loginRequestDto.getPassword(),
                user.getPassword()
        );

        // 4) 비밀번호 불일치시 예외처리
        if(!isPasswordMatch) {
            throw new CustomExceptionV2(ExceptionCodeV2.LOGIN_FAILED);
        }

        // 5) JWT 발급
        final Long userId = user.getId();
        final Role role = user.getRole();
        final String accessToken = jwtProvider.createAccessToken(userId, role);
        final Long expTime = jwtProvider.getAccessTokenExpirationSeconds();
        final String refreshToken = jwtProvider.createRefreshToken(userId);

        // 6) DTO에 담아 리턴
        return LoginResponseDtoV2.builder()
                .role(role)
                .accessToken(accessToken)
                .expTime(expTime)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 개발용
     * @param loginRequestDto
     * @return
     */
    public LoginResponseDtoV2 signUp(final LoginRequestDtoV2 loginRequestDto) {
        final String username = loginRequestDto.getUsername();
        final String password = securityConfig.passwordEncoderV2().encode(loginRequestDto.getPassword());

        final UserV2 user = new UserV2(username, password);

        final UserV2 savedUser = userRepository.save(user);

        final Long userId = user.getId();
        final Role role = user.getRole();

        final String accessToken = jwtProvider.createAccessToken(userId, role);
        final Long expTime = jwtProvider.getAccessTokenExpirationSeconds();
        final String refreshToken = jwtProvider.createRefreshToken(userId);

        return LoginResponseDtoV2.builder()
                .role(role)
                .accessToken(accessToken)
                .expTime(expTime)
                .refreshToken(refreshToken)
                .build();
    }
}
