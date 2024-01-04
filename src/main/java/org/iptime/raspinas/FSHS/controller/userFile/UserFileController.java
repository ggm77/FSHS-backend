package org.iptime.raspinas.FSHS.controller.userFile;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userFile.request.UserFileCreateRequestDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.iptime.raspinas.FSHS.service.userFile.UserFileCreateService;
import org.iptime.raspinas.FSHS.service.userFile.UserFileDeleteService;
import org.iptime.raspinas.FSHS.service.userFile.UserFileReadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserFileController {

    private final UserFileCreateService userFileCreateService;
    private final UserFileReadService userFileReadService;
    private final UserFileDeleteService userFileDeleteService;
    private final TokenProvider tokenProvider;

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)//upload file
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "유저 파일 업로드 성공"
            )
    })
    public ResponseEntity<?> createUserFile(
            @RequestPart(value = "files") final List<MultipartFile> multipartFiles,
            @RequestPart(value = "info") final UserFileCreateRequestDto requestDto,
            @RequestHeader(value = "Authorization") final String token
    ){
        final String userId = tokenProvider.validate(token.substring(7));
        final Long id = Long.parseLong(userId);

        final List<UserFile> result = userFileCreateService.createUserFile(multipartFiles, requestDto, id);


        final URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/file/{id}")
                .buildAndExpand(result.get(0).getId())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/file/{id}")//download file
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 파일 다운로드 성공"
            )
    })
    public ResponseEntity<?> getUserFile(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token
    ){
        final String userId = tokenProvider.validate(token.substring(7));
        final Long longUserId = Long.parseLong(userId);

        return userFileReadService.readUserFile(id, longUserId);
    }

//    @PostMapping("/file/get-files")
//    public ResponseEntity getManyUserFile(@RequestHeader(value = "Authorization") String token, @RequestBody UserFileReadRequestDto requestDto){
//        String userId = tokenProvider.validate(token.substring(7));
//        Long longUserId = Long.parseLong(userId);
//
//        return userFileReadService.readManyUserFile(requestDto.getIdList(), longUserId);
//    }


    @DeleteMapping("/file/{id}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "유저 파일 삭제 성공"
            )
    })
    public ResponseEntity<?> deleteUserFile(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token
    ){

        final Long userId = Long.parseLong(
                tokenProvider.validate(token.substring(7))
        );

        userFileDeleteService.deleteUserFile(id, userId);

        return ResponseEntity.noContent().build();
    }
}
