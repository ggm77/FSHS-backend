package org.iptime.raspinas.FSHS.service.userFile;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.userFile.UserFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserFileDeleteService {

    private final UserFileRepository userFileRepository;

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public void deleteUserFile(Long fileId, Long userId){
        UserFile file;
        try{
            file = userFileRepository.findById(fileId).get();
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

        Path path = Paths.get(UserFileDirPath+file.getUrl()+file.getFileName()+"."+file.getFileExtension());



        try{
            userFileRepository.deleteById(fileId);
        } catch (NoSuchElementException e) {
            throw new CustomException(ExceptionCode.FILE_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        try {

            Tika tika = new Tika();

//            String mimeType;
//            try {
//                mimeType = tika.detect(path);
//            } catch (IOException e) {
//                throw new CustomException(ExceptionCode.FILE_MISSING);
//            }
//
//
//            //delete thumbnail file
//            if(mimeType.startsWith("image") || mimeType.startsWith("video") || mimeType.startsWith("audio")){
//                String thumbnailPath = UserFileDirPath+"/thumbnail/"+file.getUrl()+file.getFileName();
//                if(Files.exists(Paths.get(thumbnailPath+".jepg"))){
//                    Files.delete(Paths.get(thumbnailPath+".jepg"));
//                } else if(Files.exists(Paths.get(thumbnailPath+".svg"))){
//                    Files.delete(Paths.get(thumbnailPath+".svg"));
//                }
//            }

            if(file.isStreaming()){
                File hlsPath;
                hlsPath = Paths.get(UserFileDirPath+file.getUrl()+"."+file.getFileName()+"/").toFile();
                FileUtils.cleanDirectory(hlsPath);
                hlsPath.delete();
            }
            Files.delete(path);
        } catch (IOException e){
            throw new CustomException(ExceptionCode.FILE_MISSING);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }
}
