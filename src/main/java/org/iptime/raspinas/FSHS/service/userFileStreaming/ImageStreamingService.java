package org.iptime.raspinas.FSHS.service.userFileStreaming;

import org.apache.commons.io.IOUtils;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageStreamingService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public ResponseEntity<byte[]> streamingImageFile(String path, String fileName){
        try {
            InputStream image = new FileInputStream(UserFileDirPath+path+fileName);
            byte[] imageByteArray = IOUtils.toByteArray(image);
            image.close();
            return new ResponseEntity<byte[]>(imageByteArray, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        } catch (IOException e) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }

    }
}
