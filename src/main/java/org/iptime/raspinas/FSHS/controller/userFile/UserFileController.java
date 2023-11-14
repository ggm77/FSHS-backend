package org.iptime.raspinas.FSHS.controller.userFile;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userFile.request.UserFileCreateRequestDto;
import org.iptime.raspinas.FSHS.dto.userFile.request.UserFileReadRequestDto;
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
    public ResponseEntity createUserFile(@RequestPart(value = "files") List<MultipartFile> multipartFiles,
                                         @RequestPart(value = "info") UserFileCreateRequestDto requestDto,
                                         @RequestHeader(value = "Authorization") String token){

        String userId = tokenProvider.validate(token.substring(7));
        Long id = Long.parseLong(userId);

        List<UserFile> result = userFileCreateService.createUserFile(multipartFiles, requestDto, id);


        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/file/{id}")
                .buildAndExpand(result.get(0).getId())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/file/{id}")//download file
    public ResponseEntity getUserFile(@RequestHeader(value = "Authorization") String token, @PathVariable Long id){
        String userId = tokenProvider.validate(token.substring(7));
        Long longUserId = Long.parseLong(userId);

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
    public ResponseEntity deleteUserFile(@RequestHeader(value = "Authorization") String token, @PathVariable Long id){
        String userId = tokenProvider.validate(token.substring(7));
        Long longUserId = Long.parseLong(userId);
        userFileDeleteService.deleteUserFile(id, longUserId);
        return ResponseEntity.noContent().build();
    }
}
