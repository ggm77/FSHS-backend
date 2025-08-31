package org.iptime.raspinas.FSHS.domain.userFile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.iptime.raspinas.FSHS.domain.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.global.exception.CustomException;
import org.iptime.raspinas.FSHS.global.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.domain.userFile.repository.UserFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFileDeleteService {

    private final UserFileRepository userFileRepository;

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public void deleteUserFile(
            final Long fileId,
            final Long userId
    ){
        final UserFile file;
        try{
            file = userFileRepository.findById(fileId).get();
        } catch (NoSuchElementException ex) {
            throw new CustomException(ExceptionCode.FILE_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFileDeleteService.deleteUserFile message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        final Long authorId = file.getUserInfo().getId();

        //Restricting access for other users. | 다른 유저 접근 제한
        if(!authorId.equals(userId)){
            throw new CustomException(ExceptionCode.FILE_ACCESS_DENY);
        }

        final Path path = Paths.get(UserFileDirPath+file.getUrl());

        try{
            userFileRepository.deleteById(fileId);
        } catch (NoSuchElementException ex) {
            throw new CustomException(ExceptionCode.FILE_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFileDeleteService.deleteUserFile message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        try {

            final Tika tika = new Tika();

            final String mimeType;
            try {
                mimeType = tika.detect(path);
            } catch (IOException ex) {
                throw new CustomException(ExceptionCode.FILE_MISSING);
            }


            //delete thumbnail file | 썸네일 지우기
            if(file.isHasThumbnail()){
                final String thumbnailPath = UserFileDirPath+convertToThumbnailPath(file.getUrl());
                try{
                    //Handle SVG file processing. | svg 파일 예외 처리
                    if(file.getFileExtension().equals("svg")){
                        Files.delete(Paths.get(thumbnailPath.substring(0,thumbnailPath.lastIndexOf('.'))+".svg"));
                    }
                    else {
                        Files.delete(Paths.get(thumbnailPath));
                    }
                } catch (FileNotFoundException ex){
                    throw new CustomException(ExceptionCode.FILE_MISSING);
                }
            }

            //delete hls file | hls 파일 지우기
            if(file.isStreaming()){
                final File hlsPath;
                hlsPath = Paths.get(UserFileDirPath+file.getUrl().substring(0, file.getUrl().lastIndexOf("/")+1)+"."+file.getFileName()+"/").toFile();
                FileUtils.cleanDirectory(hlsPath);
                hlsPath.delete();
            }
            Files.delete(path);

        } catch (IOException ex){
            throw new CustomException(ExceptionCode.FILE_MISSING);
        } catch (Exception ex){
            log.error("UserFileDeleteService.deleteUserFile message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String convertToThumbnailPath(final String filePath){
        final Integer lastSlashIndex = filePath.lastIndexOf("/");
        final Integer lastDotIndex = filePath.lastIndexOf(".");

        return "/thumbnail"+filePath.substring(0,lastSlashIndex+1)+"s_"+filePath.substring(lastSlashIndex+1, lastDotIndex)+".jpeg";
    }
}
