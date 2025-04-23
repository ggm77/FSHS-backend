package org.iptime.raspinas.FSHS.userFile.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.userFile.adapter.inbound.dto.UserFileResponseDto;
import org.iptime.raspinas.FSHS.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.common.exception.CustomException;
import org.iptime.raspinas.FSHS.common.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.userFile.adapter.outbound.UserFileRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFileReadService {

    private final UserFileRepository userFileRepository;

    public ResponseEntity<UserFileResponseDto> readUserFileInfo(
            final Long userId,
            final Long id
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

        return ResponseEntity.ok(new UserFileResponseDto(file));
    }
}
