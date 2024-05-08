package org.iptime.raspinas.FSHS.service.userFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.dto.userFile.request.UserFileUpdateRequestDto;
import org.iptime.raspinas.FSHS.dto.userFile.response.UserFileSimpleResponseDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
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
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFileUpdateService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    private final UserFileRepository userFileRepository;


    @Transactional
    public UserFileSimpleResponseDto updateUserFile(
            final Long userId,
            final Long fileId,
            final UserFileUpdateRequestDto userFileUpdateRequestDto
    ){

        final UserFile userFile;
        final boolean isDuplicateFilePresent;

        try{
            userFile = userFileRepository.findById(fileId).get();
            isDuplicateFilePresent = userFileRepository.existsByParentIdAndOriginalFileName(userFile.getParent().getId(), userFileUpdateRequestDto.getOriginalFileName());
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFileCreateService.saveFile message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //Restrict other user access | 다른 유저 접근 방지
        if( !userFile.getUserInfo().getId().equals(userId) ){
            throw new CustomException(ExceptionCode.FILE_ACCESS_DENY);
        }

        //Restrict file name is duplicated } | 파일 이름 겹침 방지
        if(isDuplicateFilePresent){
            throw new CustomException(ExceptionCode.FILE_NAME_DUPLICATED);
        }


        userFile.setOriginalFileName(userFileUpdateRequestDto.getOriginalFileName());

        return new UserFileSimpleResponseDto(userFile);
    }
}
