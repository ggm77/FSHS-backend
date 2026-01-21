package org.iptime.raspinas.FSHS.v2.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserRequestDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserResponseDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.entity.UserV2;
import org.iptime.raspinas.FSHS.v2.domain.user.repository.UserRepositoryV2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceV2 {

    private final UserRepositoryV2 userRepository;

    /**
     * 유저 생성하는 메서드
     * @param userRequestDto 유저 정보 담긴 DTO
     * @return 저장된 유저 정보 담긴 DTO
     */
    public UserResponseDtoV2 createUser(final UserRequestDtoV2 userRequestDto) {

        // 1) 변수에 저장
        final String username = userRequestDto.getUsername();
        final String password = userRequestDto.getPassword(); //테스트 용

        // 2) 엔티티 만들기
        final UserV2 user = UserV2.builder()
                .username(username)
                .password(password)
                .build();

        // 3) DB에 저장
        final UserV2 savedUser = userRepository.save(user);

        // 4) DTO에 담아 리턴
        return UserResponseDtoV2.builder()
                .user(savedUser)
                .build();
    }

    /**
     * 유저 조회하는 메서드
     * @return 조회한 유저 DTO
     */
    public UserResponseDtoV2 getUser() {

        final UserV2 user = userRepository.findAll().get(0);

        return UserResponseDtoV2.builder()
                .user(user)
                .build();
    }
}
