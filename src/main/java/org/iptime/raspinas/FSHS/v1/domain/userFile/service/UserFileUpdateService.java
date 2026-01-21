package org.iptime.raspinas.FSHS.v1.domain.userFile.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.v1.domain.userFile.dto.UserFileUpdateRequestDto;
import org.iptime.raspinas.FSHS.v1.domain.userFile.dto.UserFileSimpleResponseDto;
import org.iptime.raspinas.FSHS.v1.domain.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.v1.global.exception.CustomException;
import org.iptime.raspinas.FSHS.v1.global.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.v1.domain.userFile.repository.UserFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

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

        //파일명 확인
        if(userFileUpdateRequestDto.getNewFileName().matches(".*[!\"#$%&'()*+.,/:;<=>?@\\[\\]\\\\|].*")){
            throw new CustomException(ExceptionCode.FILE_NAME_NOT_VALID);
        }

        final UserFile userFile;
        final boolean isDuplicateFilePresent;
        final String originalFileName;

        try{
            userFile = userFileRepository.findById(fileId).get();

            originalFileName = userFileUpdateRequestDto.getNewFileName() + "." + userFile.getFileExtension();

            isDuplicateFilePresent = userFileRepository.existsByParentIdAndOriginalFileName(userFile.getParent().getId(), originalFileName);
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

        final Boolean isFavorite = userFileUpdateRequestDto.getIsFavorite();
        final Boolean hasThumbnail = userFileUpdateRequestDto.getHasThumbnail();
        final Boolean isStreaming = userFileUpdateRequestDto.getIsStreaming();
        final Boolean isStreamingMusic = userFileUpdateRequestDto.getIsStreamingMusic();
        final Boolean isStreamingVideo = userFileUpdateRequestDto.getIsStreamingVideo();
        final Boolean isShared = userFileUpdateRequestDto.getIsShared();
        final Boolean isSecrete = userFileUpdateRequestDto.getIsSecrete();

        if( originalFileName != null && !originalFileName.isEmpty()){
            userFile.setOriginalFileName(originalFileName);
        }

        if( isFavorite != null ){
            userFile.setFavorite(isFavorite);
        }

        if( hasThumbnail != null ){
            userFile.setHasThumbnail(hasThumbnail);
        }

        if( isStreaming != null ){
            userFile.setStreaming(isStreaming);
        }

        if( isStreamingMusic != null ){
            userFile.setStreamingMusic(isStreamingMusic);
        }

        if( isStreamingVideo != null ){
            userFile.setStreamingVideo(isStreamingVideo);
        }

        if( isShared != null ){
            userFile.setShared(isShared);
        }

        if( isSecrete != null ){
            userFile.setSecrete(isSecrete);
        }

        return new UserFileSimpleResponseDto(userFile);
    }
}
