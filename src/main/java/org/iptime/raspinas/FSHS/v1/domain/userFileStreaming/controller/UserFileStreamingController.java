package org.iptime.raspinas.FSHS.v1.domain.userFileStreaming.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.v1.global.exception.CustomException;
import org.iptime.raspinas.FSHS.v1.global.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.v1.global.auth.jwt.TokenProvider;
import org.iptime.raspinas.FSHS.v1.domain.userFileStreaming.service.UserFileStreamingService;
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

    private final UserFileStreamingService userFileStreamingService;
    private final TokenProvider tokenProvider;

    //@GetMapping("/streaming-video/{token}/{path}/{fileName}")
    @GetMapping("/streaming-video/{path}/{hlsFile}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "hls 프로토콜 조회 성공"
            )
    })
    public ResponseEntity<InputStreamResource> streamVideo(
            //@PathVariable String token
            @PathVariable final String path, // ' @{userFolder}@{userFolder}@ '   @ ==> /
            @PathVariable final String hlsFile
    ){

//        String userId = tokenProvider.validate(token);
        final String changedPath = path.replaceAll("@","/");

        final String hlsFileName = hlsFile.substring(0, hlsFile.lastIndexOf("."));

        final File file = this.userFileStreamingService.getHlsFile(changedPath, hlsFileName);

        try {
            final InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-mpegURL"))
                    .body(resource);

        } catch (FileNotFoundException ex) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }
    }

    @GetMapping("/streaming-audio/{path}/{hlsFile}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "hls 프로토콜 조회 성공"
            )
    })
    public ResponseEntity<InputStreamResource> streamAudio(
            @PathVariable final String path, // ' @{userFolder}@{userFolder}@ '   @ ==> /
            @PathVariable final String hlsFile
    ){

        final String changedPath = path.replaceAll("@","/");

        final String hlsFileName = hlsFile.substring(0, hlsFile.lastIndexOf("."));

        final File file = this.userFileStreamingService.getHlsFile(changedPath, hlsFileName);

        try{
            final InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-mpegURL"))
                    .body(resource);

        } catch (FileNotFoundException ex){
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }
    }

    @GetMapping("/streaming-image")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이미지 조회 성공"
            )
    })
    public ResponseEntity<?> streamImageFile(
            @RequestHeader(value = "Authorization") final String token,
            @RequestParam final String path // ' @{userFolder}@{userFolder}@ '   @ ==> /
    ){

        final String id = tokenProvider.validate(token.substring(7));
        final String fileName = path.substring(path.lastIndexOf('/')+1);

        return userFileStreamingService.streamingImageFile(path.substring(0,path.lastIndexOf('/')+1),fileName);
    }

    @GetMapping("/streaming-thumbnail")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "썸네일 조회 성공"
            )
    })
    public ResponseEntity<?> streamThumbnailImageFile(
            @RequestParam final String path
    ){

        final String changedPath = path.substring(0, path.lastIndexOf("/")+1);

        if(path.endsWith("svg") || path.endsWith("SVG")){
            return userFileStreamingService.streamingImageFile("/thumbnail"+changedPath,"s_" + path.substring(path.lastIndexOf("/")+1));
        } else {
            return userFileStreamingService.streamingImageFile(
                    "/thumbnail"+changedPath,
                    "s_" + path.substring(
                        path.lastIndexOf("/")+1,
                        path.lastIndexOf(".")
                    ) + ".jpeg"
            );
        }

    }
}
