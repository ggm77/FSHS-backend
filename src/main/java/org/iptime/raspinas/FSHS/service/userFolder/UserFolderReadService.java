package org.iptime.raspinas.FSHS.service.userFolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.dto.userFolder.response.UserFolderResponseDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.userFile.UserFileRepository;
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
