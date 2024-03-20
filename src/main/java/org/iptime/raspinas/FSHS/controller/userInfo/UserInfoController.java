package org.iptime.raspinas.FSHS.controller.userInfo;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @GetMapping("/users/{id}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponseDto.class))
            )
    })
    public ResponseEntity<?> getUserInfo(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token
    ){

        final Long userId = Long.parseLong(
                tokenProvider.validate(token.substring(7))
        );

        //Restricting access for other users. | 다른 유저 접근 제한
        if(!userId.equals(id)) {
            throw new CustomException(ExceptionCode.TOKEN_AND_ID_NOT_MATCHED);
        }

        final UserInfoResponseDto result = userInfoService.getUserInfo(userId);

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/users/{id}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 정보 변경 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponseDto.class))
            )
    })
    public ResponseEntity<?> updateUserInfo(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token,
            @RequestBody final UserInfoRequestDto requestDto
    ){

        final Long userId = Long.parseLong(
                tokenProvider.validate(token.substring(7))
        );

        //Restricting access for other users. | 다른 유저 접근 제한
        if(!userId.equals(id)) {
            throw new CustomException(ExceptionCode.TOKEN_AND_ID_NOT_MATCHED);
        }

        UserInfoResponseDto result = userInfoService.updateUserInfo(requestDto, userId);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/users/{id}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "유저 정보 삭제 성공"
            )
    })
    public ResponseEntity<?> deleteUserInfo(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token
    ){

        final Long userId = Long.parseLong(
                tokenProvider.validate(token.substring(7))
        );

        //Restricting access for other users. | 다른 유저 접근 제한
        if(!userId.equals(id)) {
            throw new CustomException(ExceptionCode.TOKEN_AND_ID_NOT_MATCHED);
        }

        userInfoService.deleteUserInfo(userId);

        return ResponseEntity.noContent().build();
    }
}
