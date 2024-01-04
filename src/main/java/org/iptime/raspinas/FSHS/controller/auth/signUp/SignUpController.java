package org.iptime.raspinas.FSHS.controller.auth.signUp;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.auth.signUp.request.SignUpRequestDto;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;
import org.iptime.raspinas.FSHS.service.auth.AuthService;
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
