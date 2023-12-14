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
import java.util.Map;

@Service
public class ImageStreamingService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public ResponseEntity streamingImageFile(String path, String fileName){
        try {
            InputStream image = new FileInputStream(UserFileDirPath+path+fileName);
            String fileExtension = StringUtils.getFilenameExtension(fileName);
            byte[] imageByteArray = IOUtils.toByteArray(image);
            image.close();
            HttpHeaders responseHeaders = new HttpHeaders();
            if(fileExtension.equals("svg") || fileExtension.equals("SVG")){
                responseHeaders.set("content-type", "image/svg+xml");
            }
            else{
                responseHeaders.set("content-type", "image/"+fileExtension);
            }
            return new ResponseEntity<byte[]>(imageByteArray, responseHeaders, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        } catch (IOException e) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }

    }
}
