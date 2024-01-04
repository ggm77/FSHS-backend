package org.iptime.raspinas.FSHS.controller.userFileStreaming;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class UserFileStreamingController {

    private final ImageStreamingService imageStreamingService;
    private final HlsService hlsService;
    private final TokenProvider tokenProvider;

    //@GetMapping("/streaming-video/{token}/{path}/{fileName}")
    @GetMapping("/streaming-video/{userId}/{path}/{fileName}/{hlsFile}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "hls 프로토콜 조회 성공"
            )
    })
    public ResponseEntity<InputStreamResource> streamVideo(
            //@PathVariable String token
            @PathVariable final String userId,
            @PathVariable final String path, // ' @{userFolder}@{userFolder}@ '   @ ==> /
            @PathVariable final String fileName,
            @PathVariable final String hlsFile
    ){

//        String userId = tokenProvider.validate(token);
        final String changedPath = path.replaceAll("@","/");

        final File file = hlsService.getHlsFile("/"+userId+changedPath,fileName, hlsFile);

        try {
            final InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-mpegURL"))
                    .body(resource);

        } catch (FileNotFoundException ex) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }
    }

    @GetMapping("/streaming-audio/{userId}/{path}/{fileName}/{hlsFile}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "hls 프로토콜 조회 성공"
            )
    })
    public ResponseEntity<InputStreamResource> streamAudio(
            @PathVariable final String userId,
            @PathVariable final String path, // ' @{userFolder}@{userFolder}@ '   @ ==> /
            @PathVariable final String fileName,
            @PathVariable final String hlsFile
    ){

        final String changedPath = path.replaceAll("@","/");

        final File file = hlsService.getHlsFile("/"+userId+changedPath, fileName, hlsFile);

        try{
            final InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-mpegURL"))
                    .body(resource);

        } catch (FileNotFoundException ex){
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }
    }

    @GetMapping("/streaming-image/{userId}/{path}/{fileNameAndExtension}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이미지 조회 성공"
            )
    })
    public ResponseEntity<?> streamImageFile(
            @RequestHeader(value = "Authorization") final String token,
            @PathVariable final String userId,
            @PathVariable final String path, // ' @{userFolder}@{userFolder}@ '   @ ==> /
            @PathVariable final String fileNameAndExtension
    ){

        final String id = tokenProvider.validate(token.substring(7));

        //Restricting access for other users. | 다른 유저 접근 제한
        if(!id.equals(userId)){
            throw new CustomException(ExceptionCode.TOKEN_AND_ID_NOT_MATCHED);
        }

        final String changedPath = path.replaceAll("@","/");

        return imageStreamingService.streamingImageFile("/"+userId+changedPath,fileNameAndExtension);
    }
}
