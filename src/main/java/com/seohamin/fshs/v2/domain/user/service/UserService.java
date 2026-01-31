package com.seohamin.fshs.v2.domain.user.service;

import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.user.dto.UserRequestDto;
import com.seohamin.fshs.v2.domain.user.dto.UserResponseDto;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    /**
     * 유저 생성하는 메서드
     * 어드민 유저가 아닌 새로운 유저를 추가하는 메서드임
     * @param userRequestDto 유저 정보 담긴 DTO
     * @return 저장된 유저 정보 담긴 DTO
     */
    public UserResponseDto createUser(final UserRequestDto userRequestDto) {

        // 1) 비밀번호 길이 검증
        if(userRequestDto.getPassword().length() < 4 ) {
            throw new CustomException(ExceptionCode.TOO_SHORT_PASSWORD);
        }

        // 2) 변수에 저장 및 비밀번호 해싱
        final String username = userRequestDto.getUsername();
        final String password = passwordEncoder.encode(userRequestDto.getPassword());

        // 3) 유저명 겹치는지 확인
        if(userRepository.existsByUsername(username)) {
            throw new CustomException(ExceptionCode.USERNAME_DUPLICATE);
        }

        // 4) 엔티티 만들기
        final User user = User.builder()
                .username(username)
                .password(password)
                .build();

        // 5) DB에 저장
        final User savedUser = userRepository.save(user);

        // 6) DTO에 담아 리턴
        return UserResponseDto.builder()
                .user(savedUser)
                .build();
    }

    /**
     * 유저 조회하는 메서드
     * @return 조회한 유저 DTO
     */
    public UserResponseDto getUser(final Long userId) {

        // 1) 유저 조회
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 2) DTO에 담아 리턴
        return UserResponseDto.builder()
                .user(user)
                .build();
    }

    /**
     * 유저 정보 수정하는 메서드
     * @param userRequestDto 수정할 정보 담긴 DTO
     * @return 수정된 유저 정보 DTO
     */
    @Transactional
    public UserResponseDto updateUser(
            final Long userId,
            final UserRequestDto userRequestDto
    ) {

        // 1) 유저 이름 변수에 저장
        final String username = userRequestDto.getUsername();

        // 2) 비밀번호 비어있지 않으면 해싱
        final String password;
        if(userRequestDto.getPassword() != null && !userRequestDto.getPassword().isEmpty()) {

            // 비밀번호 길이 제한
            if(userRequestDto.getPassword().length() < 4 ) {
                throw new CustomException(ExceptionCode.TOO_SHORT_PASSWORD);
            }

            password = passwordEncoder.encode(userRequestDto.getPassword());
        }
        else {
            password = null;
        }

        // 3) 유저 엔티티 조회
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 유저명 비어있지 않다면 변경
        if(username != null && !username.isEmpty()) {
            user.updateUsername(username);
        }

        // 5) 비밀번호 비어있지 않다면 변경
        if(password != null) {
            user.updatePassword(password);
        }

        // 6) DTO에 담아서 리턴
        return UserResponseDto.builder()
                .user(user)
                .build();
    }

    /**
     * 유저 삭제용 메서드
     * @param userId 삭제할 유저 ID
     */
    @Transactional
    public void deleteUser(final Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * 세션 기반 인증시 스프링 시큐리티가 사용하는 메서드
     * @param username the username identifying the user whose data is required.
     * @return 스프링 시큐리티에서 사용하는 유저 객체
     */
    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {

        // 1) DB에서 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 2) 스프링 시큐리티가 사용하는 객체로 리턴
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
