package org.iptime.raspinas.FSHS.domain.userFolder.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.domain.userFolder.dto.UserFolderRequestDto;
import org.iptime.raspinas.FSHS.domain.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.domain.userInfo.domain.UserInfo;
import org.iptime.raspinas.FSHS.global.exception.CustomException;
import org.iptime.raspinas.FSHS.global.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.domain.userFile.repository.UserFileRepository;
import org.iptime.raspinas.FSHS.domain.userInfo.repository.UserInfoRepository;
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
            final Long folderId,
            final Long userId,
            final UserFolderRequestDto userFolderRequestDto
    ){

        //폴더명 확인
        if(userFolderRequestDto.getFolderName().matches(".*[!\"#$%&'()*+.,/:;<=>?@\\[\\]\\\\|].*")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
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


        final String newFolderPath = folder.getParent().getUrl()+"/"+userFolderRequestDto.getFolderName();

        final String newFolderName = userFolderRequestDto.getFolderName();
        final Integer oldFolderIndex;
        final Integer oldFolderLen;
        final UserFile userFile;
        final UserInfo userInfo;

        final boolean isDuplicated;
        try{
            isDuplicated = userFileRepository.existsByUrlAndIsDirectory(newFolderPath, true);
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


        oldFolderIndex = folder.getUrl().lastIndexOf("/") + 1;
        oldFolderLen = folder.getUrl().substring(folder.getUrl().lastIndexOf("/")+1).length();

        Path source = Paths.get(UserFileDirPath + folder.getUrl());
        Path target = Paths.get(UserFileDirPath + newFolderPath);
        Path thumbnailSource = Paths.get(UserFileDirPath + "/thumbnail" + folder.getUrl());
        Path thumbnailTarget = Paths.get(UserFileDirPath + "/thumbnail" + newFolderPath);

        try {
            Files.move(source, target);
            Files.move(thumbnailSource, thumbnailTarget);
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
               updateFolderPath(child, newName, oldNameIndex, oldNameLen);
           }
       }
    }

}
