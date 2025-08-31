package org.iptime.raspinas.FSHS.domain.auth.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.domain.auth.dto.SignUpRequestDto;
import org.iptime.raspinas.FSHS.domain.userInfo.domain.UserInfo;
import org.iptime.raspinas.FSHS.domain.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SignUpController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공"
            )
    })
    public ResponseEntity<?> signUp(
            @RequestBody final SignUpRequestDto requestDto
    ){
        final UserInfo result = authService.signUp(requestDto);

        final URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/user/{id}")
                .buildAndExpand(result.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }
}
