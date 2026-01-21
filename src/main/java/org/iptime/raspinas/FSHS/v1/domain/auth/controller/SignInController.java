package org.iptime.raspinas.FSHS.v1.domain.auth.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v1.domain.auth.dto.SignInRequestDto;
import org.iptime.raspinas.FSHS.v1.domain.auth.dto.SignInResponseDto;
import org.iptime.raspinas.FSHS.v1.domain.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SignInController {

    private final AuthService authService;

    @PostMapping("/sign-in")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = SignInResponseDto.class))
            )
    })
    public ResponseEntity<?> signIn(
            @RequestBody final SignInRequestDto requestDto
    ){
        final SignInResponseDto result = authService.signIn(requestDto);
        return ResponseEntity.ok(result);
    }
}
