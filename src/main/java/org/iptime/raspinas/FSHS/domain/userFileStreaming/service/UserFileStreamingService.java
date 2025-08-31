package org.iptime.raspinas.FSHS.domain.userFileStreaming.service;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.global.common.fileIO.HlsFile;
import org.iptime.raspinas.FSHS.global.common.fileIO.ImageFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class UserFileStreamingService {

    private final HlsFile hlsFile;
    private final ImageFile imageFile;

    public File getHlsFile(
            final String requestPath,
            final String hlsFileName
    ){
        return hlsFile.getHlsFile(requestPath, hlsFileName);
    }

    public ResponseEntity streamingImageFile(
            final String path,
            final String fileName
    ){

        final byte[] imageByteArray = imageFile.streamingImageFile(path, fileName);

        final HttpHeaders responseHeaders = new HttpHeaders();

        //Handle SVG file processing. | svg 파일 예외 처리
        if(fileName.toLowerCase().endsWith("svg")){
            responseHeaders.set("content-type", "image/svg+xml");
        }
        else{
            responseHeaders.set("content-type", "image/"+fileName.substring(fileName.lastIndexOf(".")+1));
        }

        return new ResponseEntity<byte[]>(imageByteArray, responseHeaders, HttpStatus.OK);
    }
}
