package org.iptime.raspinas.FSHS.controller.userInfo;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userInfo.request.UserInfoRequestDto;
import org.iptime.raspinas.FSHS.dto.userInfo.response.UserInfoResponseDto;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.iptime.raspinas.FSHS.service.userInfo.UserInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserInfoController {

    private final UserInfoService userInfoService;
    private final TokenProvider tokenProvider;

    @GetMapping("/user/{id}")
    public ResponseEntity getUserInfo(@RequestHeader(value = "Authorization") String token, @PathVariable Long id){
        String userId = tokenProvider.validate(token.substring(7));
        if(!userId.equals(id.toString())) {
            throw new CustomException(ExceptionCode.TOKEN_AND_ID_NOT_MATCHED);
        }
        UserInfoResponseDto result = userInfoService.getUserInfo(id);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/user/{id}")
    public ResponseEntity updateUserInfo(@RequestHeader(value = "Authorization") String token, @PathVariable Long id, @RequestBody UserInfoRequestDto requestDto){
        String userId = tokenProvider.validate(token.substring(7));
        if(!userId.equals(id.toString())){
            throw new CustomException(ExceptionCode.TOKEN_AND_ID_NOT_MATCHED);
        }
        UserInfoResponseDto result = userInfoService.updateUserInfo(requestDto, id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity deleteUserInfo(@RequestHeader(value = "Authorization") String token, @PathVariable Long id){
        String userId = tokenProvider.validate(token.substring(7));
        if(!userId.equals(id.toString())){
            throw new CustomException(ExceptionCode.TOKEN_AND_ID_NOT_MATCHED);
        }
        userInfoService.deleteUserInfo(id);
        return ResponseEntity.noContent().build();
    }
}
