package org.iptime.raspinas.FSHS.service.userFile;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userFile.request.UserFileRequestDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.userFile.UserFileRepository;
import org.iptime.raspinas.FSHS.repository.userInfo.UserInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFileService {

    private final UserFileRepository userFileRepository;
    private final UserInfoRepository userInfoRepository;

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public List<UserFile> createUserFile(List<MultipartFile> files, UserFileRequestDto requestDto, Long id){
        if(files.isEmpty()){
            throw new CustomException(ExceptionCode.FILE_NOT_UPLOADED);
        }

        List<UserFile> result = new ArrayList<>();

        UserInfo userInfo = userInfoRepository.findById(id).get();
        String filePath = generatePath(requestDto.getPath()); // ' baseDir/user/~~~ '
        boolean isSecrete = requestDto.isSecrete();

        for(MultipartFile multipartFile : files){
            if(multipartFile.isEmpty()){
                continue;
            }
            result.add(saveFile(multipartFile, filePath,userInfo, isSecrete));
        }

        return result;
    }


    private UserFile saveFile(MultipartFile file, String path, UserInfo userInfo, boolean isSecrete){
        String fileName = generateSaveFileName(file.getOriginalFilename());
        String filePath = path+"/"+fileName;
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e){
            throw new CustomException(ExceptionCode.FAILED_TO_SAVE_FILE_IN_DIR);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        UserFile fileEntity = UserFile.builder()
                .userInfo(userInfo)
                .originalFileName(file.getOriginalFilename())
                .fileName(fileName)
                .fileSize(file.getSize())
                .url(filePath)
                .isSecrete(isSecrete)
                .build();

        UserFile result;
        try{
            result = userFileRepository.save(fileEntity);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    private String generatePath(String filePath){
        String path =  UserFileDirPath + filePath; //base path + param
        File folderPath = new File(path);
        if(!folderPath.exists()){
            try{
                folderPath.mkdirs();
            } catch (Exception e){
                e.printStackTrace();
                throw new CustomException(ExceptionCode.FAILED_TO_MAKE_DIR);
            }
        }
        return folderPath.getPath();
    }

    private String generateSaveFileName(String originalFileName){
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        String extension = StringUtils.getFilenameExtension(originalFileName);
        return uuid+"."+extension;
    }


}
