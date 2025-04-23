package org.iptime.raspinas.FSHS.userFile.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.common.exception.CustomException;
import org.iptime.raspinas.FSHS.common.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.userFile.adapter.outbound.UserFileRepository;
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
@Slf4j
public class UserFileDownloadService {

    private final UserFileRepository userFileRepository;

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public ResponseEntity downloadUserFile(
            final Long id,
            final Long userId
    ){

        final UserFile file;

        try{
            file = userFileRepository.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new CustomException(ExceptionCode.FILE_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFileDownloadService.downloadUserFile message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        final Long authorId = file.getUserInfo().getId();

        //Restricting access for other users. | 다른 유저 접근 제한
        if(!authorId.equals(userId)){
            throw new CustomException(ExceptionCode.FILE_ACCESS_DENY);
        }

        //Restricting download folder | 폴더 다운로드 제한
        if(file.isDirectory()){
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }

        final String path = UserFileDirPath+file.getUrl();
        final String originalFileName = file.getOriginalFileName();
        final String encodedFileName;

        try {
            encodedFileName = URLEncoder.encode(originalFileName, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException ex) {
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        try{
            final InputStreamResource resource = new InputStreamResource(new FileInputStream(path));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .cacheControl(CacheControl.noCache())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"+encodedFileName)
                    .contentLength(file.getFileSize())
                    .body(resource);

        } catch (FileNotFoundException ex) {
            throw new CustomException(ExceptionCode.FILE_MISSING);
        } catch (Exception ex){
            log.error("UserFileDownloadService.downloadUserFile message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }
}
