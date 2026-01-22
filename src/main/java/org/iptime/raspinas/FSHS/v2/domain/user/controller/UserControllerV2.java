package org.iptime.raspinas.FSHS.v2.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserRequestDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserResponseDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.service.UserServiceV2;
import org.iptime.raspinas.FSHS.v2.global.validation.Create;
import org.iptime.raspinas.FSHS.v2.global.validation.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class UserControllerV2 {

    private final UserServiceV2 userService;

    // 추가 유저 등록하는 API
    @PostMapping("/users")
    public ResponseEntity<UserResponseDtoV2> createUser(
            @Validated(Create.class) @RequestBody final UserRequestDtoV2 userRequestDto
    ) {

        return ResponseEntity.ok().body(userService.createUser(userRequestDto));
    }

    // 유저 정보 조회하는 API
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponseDtoV2> getUser(
            @PathVariable final Long userId
    ) {

        return ResponseEntity.ok().body(userService.getUser(userId));
    }

    // 유저 정보 수정하는 API
    @PatchMapping("/users/{userId}")
    public ResponseEntity<UserResponseDtoV2> updateUser(
            @PathVariable final Long userId,
            @Validated(Update.class) @RequestBody final UserRequestDtoV2 userRequestDto
    ) {

        return ResponseEntity.ok().body(userService.updateUser(userId, userRequestDto));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable final Long userId
    ) {

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}
