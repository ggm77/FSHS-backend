package org.iptime.raspinas.FSHS.v1.global.common.fileIO;

import org.apache.commons.io.IOUtils;
import org.iptime.raspinas.FSHS.v1.global.exception.CustomException;
import org.iptime.raspinas.FSHS.v1.global.exception.constants.ExceptionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageFile {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public byte[] streamingImageFile(
            final String path,
            final String fileName
    ){

        //Validate the correctness of the provided file path. | 경로가 올바른지 체크
        if(!path.startsWith("/") || !path.endsWith("/") || path.contains(".")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        try {
            final InputStream image = new FileInputStream(UserFileDirPath+path+fileName);
            final String fileExtension = StringUtils.getFilenameExtension(fileName).toLowerCase();
            final byte[] imageByteArray = IOUtils.toByteArray(image);
            image.close();

            return imageByteArray;

        } catch (FileNotFoundException ex) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        } catch (IOException ex) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }
    }
}
