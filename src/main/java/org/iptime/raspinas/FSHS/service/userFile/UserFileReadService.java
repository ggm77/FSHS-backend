package org.iptime.raspinas.FSHS.service.userFile;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.userFile.UserFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserFileReadService {

    private final UserFileRepository userFileRepository;

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public ResponseEntity readUserFile(Long id, Long userId){
        UserFile file;

        try{
            file = userFileRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            throw new CustomException(ExceptionCode.FILE_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        Long authorId = file.getUserInfo().getId();

        if(!authorId.equals(userId)){
            throw new CustomException(ExceptionCode.FILE_ACCESS_DENY);
        }

        String path = UserFileDirPath+file.getUrl()+file.getFileName()+"."+file.getFileExtension();
        String originalFileName = file.getOriginalFileName();
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(originalFileName, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
        try{
            InputStreamResource resource = new InputStreamResource(new FileInputStream(path));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .cacheControl(CacheControl.noCache())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"+encodedFileName)
                    .body(resource);
        } catch (FileNotFoundException e) {
            throw new CustomException(ExceptionCode.FILE_MISSING);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }


}
