package org.iptime.raspinas.FSHS.service.userFileStreaming;

import org.apache.commons.io.IOUtils;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageStreamingService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public ResponseEntity streamingImageFile(
            final String path,
            final String fileName
    ){

        //Validate the correctness of the provided file path. | 경로가 올바른지 체크
        if(!path.startsWith("/") || !path.endsWith("/") || path.contains(".")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        try {
            final InputStream image = new FileInputStream(UserFileDirPath+path+fileName);
            final String fileExtension = StringUtils.getFilenameExtension(fileName);
            final byte[] imageByteArray = IOUtils.toByteArray(image);
            image.close();

            final HttpHeaders responseHeaders = new HttpHeaders();

            //Handle SVG file processing. | svg 파일 예외 처리
            if(fileExtension.equals("svg") || fileExtension.equals("SVG")){
                responseHeaders.set("content-type", "image/svg+xml");
            }
            else{
                responseHeaders.set("content-type", "image/"+fileExtension);
            }

            return new ResponseEntity<byte[]>(imageByteArray, responseHeaders, HttpStatus.OK);

        } catch (FileNotFoundException ex) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        } catch (IOException ex) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }
    }
}
