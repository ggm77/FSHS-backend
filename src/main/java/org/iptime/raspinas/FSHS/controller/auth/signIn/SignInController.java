package org.iptime.raspinas.FSHS.controller.auth.signIn;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.auth.signIn.reqeust.SignInRequestDto;
import org.iptime.raspinas.FSHS.dto.auth.signIn.response.SignInResponseDto;
import org.iptime.raspinas.FSHS.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SignInController {

    private final AuthService authService;

    @PostMapping("/signIn")
    public ResponseEntity signIn(@RequestBody SignInRequestDto requestDto){
        SignInResponseDto result = authService.signIn(requestDto);
        return ResponseEntity.ok(result);
    }
}
