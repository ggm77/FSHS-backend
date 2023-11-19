package org.iptime.raspinas.FSHS.service.userFileStreaming;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class HlsService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public File getHlsFile(String path, String name){
        return new File(UserFileDirPath+path+name);
    }
}
