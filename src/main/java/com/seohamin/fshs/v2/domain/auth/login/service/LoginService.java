package com.seohamin.fshs.v2.domain.auth.login.service;

import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.auth.login.dto.LoginRequestDto;
import com.seohamin.fshs.v2.domain.auth.login.dto.LoginResponseDto;
import com.seohamin.fshs.v2.domain.user.entity.Role;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.auth.jwt.JwtProvider;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    /**
     * 로그인 진행해서 JWT 발급하는 메서드
     * @param loginRequestDto 로그인 정보 담긴 DTO
     * @return JWT
     */
    public LoginResponseDto login(final LoginRequestDto loginRequestDto) {

        // 1) 유저명 변수에 지정
        final String username = loginRequestDto.getUsername();

        // 2) 유저명으로 유저 엔티티 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 3) 비밀번호 비교
        final boolean isPasswordMatch = passwordEncoder.matches(
                loginRequestDto.getPassword(),
                user.getPassword()
        );

        // 4) 비밀번호 불일치시 예외처리
        if(!isPasswordMatch) {
            throw new CustomException(ExceptionCode.LOGIN_FAILED);
        }

        // 5) JWT 발급
        final Long userId = user.getId();
        final Role role = user.getRole();
        final String accessToken = jwtProvider.createAccessToken(userId, role);
        final Long expTime = jwtProvider.getAccessTokenExpirationSeconds();
        final String refreshToken = jwtProvider.createRefreshToken(userId);

        // 6) DTO에 담아 리턴
        return LoginResponseDto.builder()
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
    public LoginResponseDto signUp(final LoginRequestDto loginRequestDto) {
        final String username = loginRequestDto.getUsername();
        final String password = passwordEncoder.encode(loginRequestDto.getPassword());

        final User user = new User(username, password);

        final User savedUser = userRepository.save(user);

        final Long userId = user.getId();
        final Role role = user.getRole();

        final String accessToken = jwtProvider.createAccessToken(userId, role);
        final Long expTime = jwtProvider.getAccessTokenExpirationSeconds();
        final String refreshToken = jwtProvider.createRefreshToken(userId);

        return LoginResponseDto.builder()
                .role(role)
                .accessToken(accessToken)
                .expTime(expTime)
                .refreshToken(refreshToken)
                .build();
    }
}
