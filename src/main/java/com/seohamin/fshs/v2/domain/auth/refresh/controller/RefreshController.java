package com.seohamin.fshs.v2.domain.auth.refresh.controller;

import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.auth.login.dto.LoginResponseDto;
import com.seohamin.fshs.v2.domain.auth.refresh.dto.RefreshRequestDto;
import com.seohamin.fshs.v2.domain.auth.refresh.service.RefreshService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class RefreshController {

    private final RefreshService refreshService;

    // 리프레시 토큰으로 JWT 새로 발급하는 API
    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponseDto> refresh(
            @Validated @RequestBody RefreshRequestDto refreshRequestDto
    ) {

        return ResponseEntity.ok().body(refreshService.refresh(refreshRequestDto));
    }
}
