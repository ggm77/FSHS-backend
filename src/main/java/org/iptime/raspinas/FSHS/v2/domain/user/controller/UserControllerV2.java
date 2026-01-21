package org.iptime.raspinas.FSHS.v2.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserRequestDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.dto.UserResponseDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.user.service.UserServiceV2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class UserControllerV2 {

    private final UserServiceV2 userService;

    @PostMapping("/user")
    public ResponseEntity<UserResponseDtoV2> createUser(
            @RequestBody final UserRequestDtoV2 userRequestDto
    ) {

        return ResponseEntity.ok().body(userService.createUser(userRequestDto));
    }

    @GetMapping("/user")
    public ResponseEntity<UserResponseDtoV2> getUser() {

        return ResponseEntity.ok().body(userService.getUser());
    }
}
