package org.iptime.raspinas.FSHS.service.userFileStreaming;

import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class HlsService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public File getHlsFile(
            final String requestPath,
            final String hlsFile
    ){

        final String path = requestPath.substring(0, requestPath.lastIndexOf("."));
        final Integer lastSlashIndex = path.lastIndexOf('/');

        //Validate the correctness of the provided file path. | 경로가 올바른지 체크
        if(path.contains(".")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        //Add extension based on file type | 파일 유형에 따라 확장자 추가
        if(hlsFile.contains("_")){
            return new File(UserFileDirPath+path.substring(0, lastSlashIndex+1)+"."+path.substring(lastSlashIndex+1)+"/"+hlsFile+".ts");
        } else {
            return new File(UserFileDirPath+path.substring(0, lastSlashIndex+1)+"."+path.substring(lastSlashIndex+1)+"/"+hlsFile+".m3u8");
        }

    }
}
