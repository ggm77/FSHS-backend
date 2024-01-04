package org.iptime.raspinas.FSHS.service.userFileStreaming;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class HlsService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public File getHlsFile(
            final String path,
            final String name,
            final String hlsFile
    ){
        return new File(UserFileDirPath+path+"."+name+"/"+hlsFile);
    }
}
