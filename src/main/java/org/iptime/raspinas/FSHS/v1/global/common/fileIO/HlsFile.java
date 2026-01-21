package org.iptime.raspinas.FSHS.v1.global.common.fileIO;

import org.iptime.raspinas.FSHS.v1.global.exception.CustomException;
import org.iptime.raspinas.FSHS.v1.global.exception.constants.ExceptionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class HlsFile {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public File getHlsFile(
            final String requestPath,
            final String hlsFileName
    ){

        final String path = requestPath.substring(0, requestPath.lastIndexOf("."));
        final Integer lastSlashIndex = path.lastIndexOf('/');

        //Validate the correctness of the provided file path. | 경로가 올바른지 체크
        if(path.contains(".")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        //Add extension based on file type | 파일 유형에 따라 확장자 추가
        if(hlsFileName.contains("_")){
            return new File(UserFileDirPath+path.substring(0, lastSlashIndex+1)+"."+path.substring(lastSlashIndex+1)+"/"+hlsFileName+".ts");
        } else {
            return new File(UserFileDirPath+path.substring(0, lastSlashIndex+1)+"."+path.substring(lastSlashIndex+1)+"/"+hlsFileName+".m3u8");
        }

    }
}
