package org.iptime.raspinas.FSHS.v2.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserRequestDto;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserResponseDto;
import org.iptime.raspinas.FSHS.v2.domain.user.service.UserService;
import org.iptime.raspinas.FSHS.v2.global.validation.Create;
import org.iptime.raspinas.FSHS.v2.global.validation.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class UserController {

    private final UserService userService;

    // 추가 유저 등록하는 API
    @PostMapping("/users")
    public ResponseEntity<UserResponseDto> createUser(
            @Validated(Create.class) @RequestBody final UserRequestDto userRequestDto
    ) {

        return ResponseEntity.ok().body(userService.createUser(userRequestDto));
    }

    // 유저 정보 조회하는 API
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponseDto> getUser(
            @PathVariable final Long userId
    ) {

        return ResponseEntity.ok().body(userService.getUser(userId));
    }

    // 유저 정보 수정하는 API
    @PatchMapping("/users/{userId}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable final Long userId,
            @Validated(Update.class) @RequestBody final UserRequestDto userRequestDto
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
