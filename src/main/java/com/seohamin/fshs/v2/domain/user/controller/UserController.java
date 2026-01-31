package com.seohamin.fshs.v2.domain.user.controller;

import com.seohamin.fshs.v2.global.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.user.dto.UserRequestDto;
import com.seohamin.fshs.v2.domain.user.dto.UserResponseDto;
import com.seohamin.fshs.v2.domain.user.service.UserService;
import com.seohamin.fshs.v2.global.validation.Create;
import com.seohamin.fshs.v2.global.validation.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class UserController {

    private final UserService userService;
    private final SessionUtil sessionUtil;

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
            @Validated(Update.class) @RequestBody final UserRequestDto userRequestDto,
            final HttpServletRequest request
    ) {

        final UserResponseDto userResponseDto = userService.updateUser(userId, userRequestDto);

        if(isSecuritySensitiveChange(userRequestDto)) {
            sessionUtil.forceLogout(request);
        }

        return ResponseEntity.ok().body(userResponseDto);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable final Long userId,
            final HttpServletRequest request
    ) {

        userService.deleteUser(userId);

        sessionUtil.forceLogout(request);

        return ResponseEntity.noContent().build();
    }

    // 요청 DTO에 username이나 password가 있는지 확인하는 메서드
    private boolean isSecuritySensitiveChange(final UserRequestDto dto) {
        boolean isPasswordChanged = dto.getPassword() != null && !dto.getPassword().isEmpty();
        boolean isUsernameChanged = dto.getUsername() != null && !dto.getUsername().isEmpty();
        return isPasswordChanged || isUsernameChanged;
    }
}
