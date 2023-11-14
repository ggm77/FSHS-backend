package org.iptime.raspinas.FSHS.controller.userFile;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userFile.request.UserFileRequestDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.iptime.raspinas.FSHS.service.userFile.UserFileService;
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

    private final UserFileService userFileService;
    private final TokenProvider tokenProvider;

    @PostMapping("/file")
    public ResponseEntity createUserFile(@RequestPart(value = "files") List<MultipartFile> multipartFiles,
                                         @RequestHeader(value = "Authorization") String token,
                                         @RequestPart(value = "info") UserFileRequestDto requestDto){

        String userId = tokenProvider.validate(token.substring(7));
        Long id = Long.parseLong(userId);

        List<UserFile> result = userFileService.createUserFile(multipartFiles, requestDto, id);


        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/file/{id}")
                .buildAndExpand(result.get(0).getId())
                .toUri();
        return ResponseEntity.created(location).build();
    }
}
