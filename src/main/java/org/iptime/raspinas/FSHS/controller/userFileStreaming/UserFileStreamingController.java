package org.iptime.raspinas.FSHS.controller.userFileStreaming;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.iptime.raspinas.FSHS.service.userFileStreaming.HlsService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class UserFileStreamingController {

    private final HlsService hlsService;
    private final TokenProvider tokenProvider;
    @GetMapping("/hls/{token}/{path}/{fileName}")
    public ResponseEntity<InputStreamResource> getHlsFile(
            @PathVariable String token,
            @PathVariable String path, // ' @{userFolder}@{userFolder}@ '   @ == /
            @PathVariable String fileName
    ){
        String userId = tokenProvider.validate(token);
        path = path.replaceAll("@","/");
        File file = hlsService.getHlsFile("/"+userId+path+"/",fileName);
        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-mpegURL"))
                    .body(resource);
        } catch (FileNotFoundException e) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }
    }
}
