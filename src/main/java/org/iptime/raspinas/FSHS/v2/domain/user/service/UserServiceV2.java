package org.iptime.raspinas.FSHS.v2.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserRequestDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserResponseDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.entity.UserV2;
import org.iptime.raspinas.FSHS.v2.domain.user.repository.UserRepositoryV2;
import org.iptime.raspinas.FSHS.v2.global.config.SecurityConfigV2;
import org.iptime.raspinas.FSHS.v2.global.exception.CustomExceptionV2;
import org.iptime.raspinas.FSHS.v2.global.exception.constants.ExceptionCodeV2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceV2 {

//    private final PasswordEncoder passwordEncoder; v1 제거 후 변경
    private final SecurityConfigV2 securityConfig;
    private final UserRepositoryV2 userRepository;

    /**
     * 유저 생성하는 메서드
     * 어드민 유저가 아닌 새로운 유저를 추가하는 메서드임
     * @param userRequestDto 유저 정보 담긴 DTO
     * @return 저장된 유저 정보 담긴 DTO
     */
    public UserResponseDtoV2 createUser(final UserRequestDtoV2 userRequestDto) {

        // 1) 비밀번호 길이 검증
        if(userRequestDto.getPassword().length() < 4 ) {
            throw new CustomExceptionV2(ExceptionCodeV2.TOO_SHORT_PASSWORD);
        }

        // 2) 변수에 저장 및 비밀번호 해싱
        final String username = userRequestDto.getUsername();
//        final String password = passwordEncoder.encode(userRequestDto.getPassword()); v1 제거 후 변경
        final String password = securityConfig.passwordEncoderV2().encode(userRequestDto.getPassword());

        // 3) 유저명 겹치는지 확인
        if(userRepository.existsByUsername(username)) {
            throw new CustomExceptionV2(ExceptionCodeV2.USERNAME_DUPLICATE);
        }

        // 4) 엔티티 만들기
        final UserV2 user = UserV2.builder()
                .username(username)
                .password(password)
                .build();

        // 5) DB에 저장
        final UserV2 savedUser = userRepository.save(user);

        // 6) DTO에 담아 리턴
        return UserResponseDtoV2.builder()
                .user(savedUser)
                .build();
    }

    /**
     * 유저 조회하는 메서드
     * @return 조회한 유저 DTO
     */
    public UserResponseDtoV2 getUser(final Long userId) {

        // 1) 유저 조회
        final UserV2 user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomExceptionV2(ExceptionCodeV2.USER_NOT_EXIST));

        // 2) DTO에 담아 리턴
        return UserResponseDtoV2.builder()
                .user(user)
                .build();
    }

    /**
     * 유저 정보 수정하는 메서드
     * @param userRequestDto 수정할 정보 담긴 DTO
     * @return 수정된 유저 정보 DTO
     */
    @Transactional
    public UserResponseDtoV2 updateUser(
            final Long userId,
            final UserRequestDtoV2 userRequestDto
    ) {

        // 1) 유저 이름 변수에 저장
        final String username = userRequestDto.getUsername();

        // 2) 비밀번호 비어있지 않으면 해싱
        final String password;
        if(userRequestDto.getPassword() != null && !userRequestDto.getPassword().isEmpty()) {

            // 비밀번호 길이 제한
            if(userRequestDto.getPassword().length() < 4 ) {
                throw new CustomExceptionV2(ExceptionCodeV2.TOO_SHORT_PASSWORD);
            }

            password = securityConfig.passwordEncoderV2().encode(userRequestDto.getPassword());
        }
        else {
            password = null;
        }

        // 3) 유저 엔티티 조회
        final UserV2 user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomExceptionV2(ExceptionCodeV2.USER_NOT_EXIST));

        // 4) 유저명 비어있지 않다면 변경
        if(username != null && !username.isEmpty()) {
            user.updateUsername(username);
        }

        // 5) 비밀번호 비어있지 않다면 변경
        if(password != null) {
            user.updatePassword(password);
        }

        // 6) DTO에 담아서 리턴
        return UserResponseDtoV2.builder()
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
}
