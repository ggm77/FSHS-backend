package com.seohamin.fshs.v2.domain.user.controller;

import com.seohamin.fshs.v2.domain.user.dto.UserRootFolderRequestDto;
import com.seohamin.fshs.v2.domain.user.dto.UserShareResponseDto;
import com.seohamin.fshs.v2.domain.user.dto.UserUpdateRequestDto;
import com.seohamin.fshs.v2.global.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.user.dto.UserRequestDto;
import com.seohamin.fshs.v2.domain.user.dto.UserResponseDto;
import com.seohamin.fshs.v2.domain.user.service.UserService;
import com.seohamin.fshs.v2.global.validation.Create;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
            @AuthenticationPrincipal final UserDetails userDetails,
            @Validated(Create.class) @RequestBody final UserRequestDto userRequestDto
    ) {

        return ResponseEntity.ok().body(userService.createUser(userRequestDto, userDetails.getAuthorities()));
    }

    // 유저 정보 조회하는 API
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponseDto> getUser(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long userId
    ) {

        return ResponseEntity.ok().body(userService.getUser(userId, userDetails.getUsername()));
    }

    // 유저 정보 수정하는 API
    @PatchMapping("/users/{userId}")
    public ResponseEntity<UserResponseDto> updateUser(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long userId,
            @RequestBody final UserUpdateRequestDto userRequestDto,
            final HttpServletRequest request
    ) {

        final UserResponseDto userResponseDto = userService.updateUser(userId, userRequestDto, userDetails.getUsername());

        if(isSecuritySensitiveChange(userRequestDto)) {
            SessionUtil.forceLogout(request);
        }

        return ResponseEntity.ok().body(userResponseDto);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long userId,
            final HttpServletRequest request
    ) {

        userService.deleteUser(userId, userDetails.getAuthorities());

        SessionUtil.forceLogout(request);

        return ResponseEntity.noContent().build();
    }

    // 특정 유저에게 루트 폴더 지정해주는 API (어드민만 사용 가능)
    @PostMapping("/users/{userId}/root-folder")
    public ResponseEntity<Void> setRootFolder(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long userId,
            @RequestBody final UserRootFolderRequestDto userRootFolderRequestDto
    ) {

        userService.setRootFolder(userId, userRootFolderRequestDto, userDetails.getAuthorities());

        return ResponseEntity.noContent().build();
    }

    // 공유한 파일 목록 조회하는 API
    @GetMapping("/users/{userId}/shares")
    public ResponseEntity<UserShareResponseDto> getUserShare(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long userId
    ) {

        return ResponseEntity.ok(userService.getUserShare(userId, userDetails.getUsername()));
    }

    // 요청 DTO에 username이나 password가 있는지 확인하는 메서드
    private boolean isSecuritySensitiveChange(final UserUpdateRequestDto dto) {
        boolean isPasswordChanged = dto.newPassword() != null && !dto.newPassword().isEmpty();
        boolean isUsernameChanged = dto.username() != null && !dto.username().isEmpty();
        return isPasswordChanged || isUsernameChanged;
    }
}
