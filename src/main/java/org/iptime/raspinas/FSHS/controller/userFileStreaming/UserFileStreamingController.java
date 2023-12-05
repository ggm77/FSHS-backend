package org.iptime.raspinas.FSHS.controller.userFileStreaming;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.iptime.raspinas.FSHS.service.userFileStreaming.HlsService;
import org.iptime.raspinas.FSHS.service.userFileStreaming.ImageStreamingService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Path;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class UserFileStreamingController {

    private final ImageStreamingService imageStreamingService;
    private final HlsService hlsService;
    private final TokenProvider tokenProvider;

    //@GetMapping("/streaming-video/{token}/{path}/{fileName}")
    @GetMapping("/streaming-video/{userId}/{path}/{fileName}/{hlsFile}")
    public ResponseEntity<InputStreamResource> getHlsFile(
            //@PathVariable String token
            @PathVariable String userId,
            @PathVariable String path, // ' @{userFolder}@{userFolder}@ '   @ ==> /
            @PathVariable String fileName,
            @PathVariable String hlsFile
    ){
//        String userId = tokenProvider.validate(token);
        path = path.replaceAll("@","/");
        File file = hlsService.getHlsFile("/"+userId+path,fileName, hlsFile);
        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-mpegURL"))
                    .body(resource);
        } catch (FileNotFoundException e) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }
    }

    @GetMapping(value = "/streaming-image/{userId}/{path}/{fileName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImageFile(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String userId,
            @PathVariable String path, // ' @{userFolder}@{userFolder}@ '   @ ==> /
            @PathVariable String fileName
    ){
        String id = tokenProvider.validate(token.substring(7));
        if(!id.equals(userId)){
            throw new CustomException(ExceptionCode.TOKEN_AND_ID_NOT_MATCHED);
        }
        path = path.replaceAll("@","/");
        return imageStreamingService.streamingImageFile("/"+userId+path,fileName);
    }
}
