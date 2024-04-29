package org.iptime.raspinas.FSHS.service.userFolder;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFolderCreateService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    private final UserFileRepository userFileRepository;
    private final UserInfoRepository userInfoRepository;

    public UserFile createUserFolder(
            final UserFolderRequestDto userFolderRequestDto,
            final Long userId
    ){

        //Validate the correctness of the provided file path. | 경로가 올바른지 체크(형식 체크)
        if(!userFolderRequestDto.getPath().startsWith("/") || userFolderRequestDto.getPath().contains(".")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        final String requestPath;
        //파일 경로 마지막 '/' 없애기로 통일
        if (userFolderRequestDto.getPath().endsWith("/")) {
            requestPath = userFolderRequestDto.getPath().substring(0, userFolderRequestDto.getPath().length() - 1);
        }
        else {
            requestPath = userFolderRequestDto.getPath();
        }



        final String path = "/" + userId + requestPath;
        final String parentPath = getParentPath(path, userId);
        final String folderName = getFolderName(requestPath);
        final UserFile parentFile;
        final UserInfo userInfo;

        try {
            userInfo = userInfoRepository.findById(userId).get();
            parentFile = userFileRepository.findByUrlAndIsDirectory(parentPath, true);
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
                .url(parentPath+"/")
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

    private String getFolderName(final String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        return path.substring(lastSlashIndex+1);
    }
}
