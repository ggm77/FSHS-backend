package org.iptime.raspinas.FSHS.v2.domain.auth.login.controller;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v2.domain.auth.login.dto.LoginRequestDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.auth.login.dto.LoginResponseDtoV2;
import org.iptime.raspinas.FSHS.v2.domain.auth.login.service.LoginServiceV2;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class LoginControllerV2 {

    private final LoginServiceV2 loginService;

    // 로그인하는 API
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDtoV2> login(
            @Validated @RequestBody final LoginRequestDtoV2 loginRequestDto
    ) {

        return ResponseEntity.ok().body(loginService.login(loginRequestDto));
    }

    // 개발용 회원가입 API
    @PostMapping("/auth/signup")
    public ResponseEntity<LoginResponseDtoV2> signup(
            @Validated @RequestBody final LoginRequestDtoV2 loginRequestDto
    ) {
        return ResponseEntity.ok().body(loginService.signUp(loginRequestDto));
    }
}
