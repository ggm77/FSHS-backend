package com.seohamin.fshs.v2.domain.auth.account.controller;

import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.auth.account.dto.SignUpRequestDto;
import com.seohamin.fshs.v2.domain.auth.account.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class AccountController {

    private final AccountService accountService;

    // 개발용 회원가입 API
    @PostMapping("/auth/signup")
    public ResponseEntity<Void> signup(
            @Validated @RequestBody final SignUpRequestDto signUpRequestDto
    ) {

        accountService.signUp(signUpRequestDto);
        return ResponseEntity.noContent().build();
    }
}
