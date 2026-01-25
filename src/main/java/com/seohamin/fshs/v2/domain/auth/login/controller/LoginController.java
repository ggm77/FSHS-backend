package com.seohamin.fshs.v2.domain.auth.login.controller;

import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.auth.login.dto.LoginRequestDto;
import com.seohamin.fshs.v2.domain.auth.login.dto.LoginResponseDto;
import com.seohamin.fshs.v2.domain.auth.login.service.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class LoginController {

    private final LoginService loginService;

    // 로그인하는 API
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDto> login(
            @Validated @RequestBody final LoginRequestDto loginRequestDto
    ) {

        return ResponseEntity.ok().body(loginService.login(loginRequestDto));
    }

    // 개발용 회원가입 API
    @PostMapping("/auth/signup")
    public ResponseEntity<LoginResponseDto> signup(
            @Validated @RequestBody final LoginRequestDto loginRequestDto
    ) {
        return ResponseEntity.ok().body(loginService.signUp(loginRequestDto));
    }
}
