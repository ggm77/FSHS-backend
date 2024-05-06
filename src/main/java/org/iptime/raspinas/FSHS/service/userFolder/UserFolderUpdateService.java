package org.iptime.raspinas.FSHS.service.userFolder;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.dto.userFolder.request.UserFolderRequestDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.userFile.UserFileRepository;
import org.iptime.raspinas.FSHS.repository.userInfo.UserInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFolderUpdateService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    private final UserFileRepository userFileRepository;
    private final UserInfoRepository userInfoRepository;

    @Transactional
    public Optional<UserFile> updateUserFolder(
            final UserFolderRequestDto userFolderRequestDto,
            final Long folderId,
            final Long userId
    ){

        //Validate the correctness of the provided file path. | 경로가 올바른지 체크(형식 체크)
        if(!userFolderRequestDto.getPath().startsWith("/") || userFolderRequestDto.getPath().contains(".")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        final String newFolderPath;
        //파일 경로 마지막 '/' 없애기로 통일
        if (userFolderRequestDto.getPath().endsWith("/")) {
            newFolderPath = userFolderRequestDto.getPath().substring(0, userFolderRequestDto.getPath().length() - 1);
        }
        else {
            newFolderPath = userFolderRequestDto.getPath();
        }

        final String newFolderName = newFolderPath.substring(newFolderPath.lastIndexOf("/")+1);
        final Integer oldFolderIndex;
        final Integer oldFolderLen;
        final UserFile userFile;
        final UserInfo userInfo;

        final boolean isDuplicated;
        try{
            isDuplicated = userFileRepository.existsByUrlAndIsDirectory("/" + userId + newFolderPath, true);
            userInfo = userInfoRepository.findById(userId).get();
            userFile = userFileRepository.findById(folderId).get();
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFolderUpdateService.updateUserFolder message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //중복 폴더 예외 처리
        if(isDuplicated){
            throw new CustomException(ExceptionCode.DIR_ALREADY_EXIST);
        }

        //check validation | 권한 체크
        if( !userInfo.getId().equals(userFile.getUserInfo().getId()) ){
            throw new CustomException(ExceptionCode.FILE_ACCESS_DENY);
        }


        final UserFile folder;
        try{
            folder = userFileRepository.findById(folderId).get();
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFolderUpdateService.updateUserFolder message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //대상 폴더가 잘못된 경우 제외
        if( !getParentPath(folder.getUrl().substring(userId.toString().length()+1), userId).equals(getParentPath(userFolderRequestDto.getPath(), userId))){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        oldFolderIndex = folder.getUrl().lastIndexOf("/") + 1;
        oldFolderLen = folder.getUrl().substring(folder.getUrl().lastIndexOf("/")+1).length();

        Path source = Paths.get(UserFileDirPath + folder.getUrl());
        Path target = Paths.get(UserFileDirPath + "/"+userId+newFolderPath);

        try {
            Files.move(source, target);
        } catch (IOException ex) {
            log.error("UserFolderUpdateService.updateUserFolder message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.FAILED_TO_MAKE_DIR);
        }

        folder.setFileName(newFolderName);
        folder.setOriginalFileName(newFolderName);
        folder.setUrl(newFolderPath);
        if (folder.getChildren() != null && !folder.getChildren().isEmpty()) {
            for (UserFile child : folder.getChildren()) {
            updateFolderPath(child, newFolderName, oldFolderIndex, oldFolderLen);
            }
        }

        return Optional.ofNullable(folder);
    }

    private void updateFolderPath(
            final UserFile folder,
            final String newName,
            final Integer oldNameIndex,
            final Integer oldNameLen
    ) {
       final String oldPath = folder.getUrl();
       final String newPath = oldPath.substring(0,oldNameIndex)+newName+oldPath.substring(oldNameIndex+oldNameLen);
       folder.setUrl(newPath);

       if (folder.getChildren() != null && !folder.getChildren().isEmpty()) {
           for (UserFile child : folder.getChildren()) {
               updateFolderPath(child, newName, oldNameIndex, oldNameIndex);
           }
       }
    }

    private String getParentPath(final String path, final Long userId) {
        final int lastSlashIndex = path.lastIndexOf('/');
        final String result = lastSlashIndex > 0 ? path.substring(0, lastSlashIndex) : "";

        //root폴더 예외 처리
        if(result.equals('/')){
            return "/" + userId;
        }
        else {
            return result;
        }
    }
}
