package org.iptime.raspinas.FSHS.service.userFolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFolderDeleteService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    private final UserInfoRepository userInfoRepository;
    private final UserFileRepository userFileRepository;

    public void deleteUserFolder(
            final Long folderId,
            final Long userId
    ){

        final UserInfo userInfo;
        final UserFile userFile;

        try {
            userInfo = userInfoRepository.findById(userId).get();
            userFile = userFileRepository.findById(folderId).get();
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFolderService.createUserFolder message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //check validation | 권한 체크
        if( !userInfo.getId().equals(userFile.getUserInfo().getId()) ){
            throw new CustomException(ExceptionCode.FILE_ACCESS_DENY);
        }

        try {
            userFileRepository.deleteById(folderId);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFolderService.createUserFolder message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        final String folderPath = UserFileDirPath + "/" + userId + userFile.getUrl();
        final String thumbnailFolderPath = UserFileDirPath + "/thumbnail/" + userId + userFile.getUrl();

        folderDelete(folderPath);
        folderDelete(thumbnailFolderPath);
    }

    private void folderDelete(
            final String path
    ){

        Path dir = Paths.get(path);

        try {
            // 재귀적으로 모든 파일과 서브폴더 삭제
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 파일 삭제
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    // 디렉토리 삭제
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException ex) {
            log.error("UserFolderDeleteService.folderDelete message:{}", ex.getMessage(), ex);
            throw new CustomException(ExceptionCode.FAILED_TO_DELETE_FOLDER);
        }
    }
}
