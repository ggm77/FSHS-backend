package org.iptime.raspinas.FSHS.v1.domain.auth.controller;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v1.domain.auth.dto.RefreshTokenRequestDto;
import org.iptime.raspinas.FSHS.v1.domain.auth.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping(value = "/refresh-token")
    public ResponseEntity<?> refreshToken(
            @RequestBody final RefreshTokenRequestDto refreshTokenRequestDto
    ){

        return ResponseEntity.ok(
                refreshTokenService.refreshToken(refreshTokenRequestDto)
        );
    }
}
