package com.seohamin.fshs.v2.domain.auth.account.service;

import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.auth.account.dto.SignUpRequestDto;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    /**
     * 개발용 회원 가입용 메서드
     * @param signUpRequestDto 회원가입 정보 담긴 DTO
     */
    public void signUp(final SignUpRequestDto signUpRequestDto) {

        // 1) 회원가입 정보 변수 저장
        final String username = signUpRequestDto.getUsername();
        final String password = passwordEncoder.encode(signUpRequestDto.getPassword());

        // 2) 닉네임 중복 확인
        if(userRepository.existsByUsername(username)) {
            throw new CustomException(ExceptionCode.USERNAME_DUPLICATE);
        }

        // 3) 유저 엔티티 생성
        final User user = new User(username, password);

        // 4) 유저 저장
        userRepository.save(user);
    }
}
