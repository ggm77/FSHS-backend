package org.iptime.raspinas.FSHS.userFolder.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.userFolder.adapter.inbound.dto.UserFolderRequestDto;
import org.iptime.raspinas.FSHS.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.userInfo.domain.UserInfo;
import org.iptime.raspinas.FSHS.common.exception.CustomException;
import org.iptime.raspinas.FSHS.common.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.userFile.adapter.outbound.UserFileRepository;
import org.iptime.raspinas.FSHS.userInfo.adapter.outbound.UserInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFolderCreateService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    private final UserFileRepository userFileRepository;
    private final UserInfoRepository userInfoRepository;

    public UserFile createUserFolder(
            final Long fileId,
            final Long userId,
            final UserFolderRequestDto userFolderRequestDto
    ){

        final String folderName = userFolderRequestDto.getFolderName();

        //폴더명 확인
        if(folderName.matches(".*[!\"#$%&'()*+.,/:;<=>?@\\[\\]\\\\|].*")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        final UserFile parentFile;
        try{
            parentFile = userFileRepository.findById(fileId).get();
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFolderService.createUserFolder message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }


        final String path = parentFile.getUrl()+"/"+userFolderRequestDto.getFolderName();
        final UserInfo userInfo;

        try {
            userInfo = userInfoRepository.findById(userId).get();
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFolderService.createUserFolder message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //부모 파일 존재여부 확인
        if(parentFile == null){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        final Path folderPath = Paths.get(UserFileDirPath + path);
        final Path thumbnailFolderPath = Paths.get(UserFileDirPath + "/thumbnail" + path);
        try {
            if(!Files.exists(folderPath)){
                Files.createDirectories(folderPath);
            }
            else {
                throw new CustomException(ExceptionCode.DIR_ALREADY_EXIST);
            }
            if(!Files.exists(thumbnailFolderPath)){
                Files.createDirectories(thumbnailFolderPath);
            }
            else {
                throw new CustomException(ExceptionCode.DIR_ALREADY_EXIST);
            }
        } catch (IOException ex){
            log.error("UserFolderService.createUserFolder message:{}", ex.getMessage(), ex);
            throw new CustomException(ExceptionCode.FAILED_TO_MAKE_DIR);
        }

        final UserFile folder = UserFile.builder()
                .userInfo(userInfo)
                .originalFileName(folderName)
                .fileName(folderName)
                .url(path)
                .isDirectory(true)
                .isStreaming(false)
                .isSecrete(false)
                .parent(parentFile)
                .build();

        try {
            return userFileRepository.save(folder);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFolderService.createUserFolder message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }
}
