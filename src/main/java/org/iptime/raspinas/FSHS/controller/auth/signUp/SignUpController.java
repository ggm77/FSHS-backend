package org.iptime.raspinas.FSHS.controller.auth.signUp;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.auth.signUp.request.SignUpRequestDto;
import org.iptime.raspinas.FSHS.entity.user.UserInfo;
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

    @PostMapping("/signUp")
    public ResponseEntity signUp(@RequestBody SignUpRequestDto requestDto){
        UserInfo result = authService.signUp(requestDto);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/user/{id}")
                .buildAndExpand(result.getId())
                .toUri();
        return ResponseEntity.created(location).build();
    }
}
