package org.iptime.raspinas.FSHS.userFolder.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.common.exception.CustomException;
import org.iptime.raspinas.FSHS.common.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.userFile.adapter.outbound.UserFileRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFolderReadService {

    private final UserFileRepository userFileRepository;

    public Optional<UserFile> getUserAllFile(
            final Long userId
    ){

        final UserFile rootFile;

        try {
            rootFile = userFileRepository.findByUrlAndIsDirectory("/" + userId, true);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("AuthService.signUp message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        return Optional.ofNullable(rootFile);
    }
}
