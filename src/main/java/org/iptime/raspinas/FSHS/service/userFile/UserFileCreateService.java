package org.iptime.raspinas.FSHS.service.userFile;

import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnailator;
import org.apache.tika.Tika;
import org.iptime.raspinas.FSHS.config.FFmpegConfig;
import org.iptime.raspinas.FSHS.dto.userFile.request.UserFileCreateRequestDto;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFileCreateService {

    private final UserFileRepository userFileRepository;
    private final UserInfoRepository userInfoRepository;
    private final FFmpegConfig fFmpegConfig;

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public List<UserFile> createUserFile(List<MultipartFile> files, UserFileCreateRequestDto requestDto, Long id){
        if(files.isEmpty()){
            throw new CustomException(ExceptionCode.FILE_NOT_UPLOADED);
        }
        if(!requestDto.getPath().startsWith("/") || !requestDto.getPath().endsWith("/")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        List<UserFile> result = new ArrayList<>();

        UserInfo userInfo = userInfoRepository.findById(id).get();
        String filePath = generatePath(UserFileDirPath+"/"+id+requestDto.getPath()); // input : ' /{folderName}/{folderName}/ '   -->>  ' /userId/{folderName}/{folderName}/ '
        String thumbnailPath = generatePath(UserFileDirPath+"/thumbnail/"+id+requestDto.getPath());
        boolean isSecrete = requestDto.isSecrete();

        for(MultipartFile multipartFile : files){
            if(multipartFile.isEmpty()){
                continue;
            }
            result.add(saveFile(multipartFile, filePath, thumbnailPath, userInfo, isSecrete));
        }

        return result;
    }


    private UserFile saveFile(MultipartFile file, String path, String thumbnailPath, UserInfo userInfo, boolean isSecrete){
        String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = generateSaveFileName();//uuid
        String filePath = path+fileName+"."+fileExtension;
        boolean isStreaming = false;
        File saveFile = new File(filePath);
        try { // saving file
            file.transferTo(saveFile);
        } catch (IOException e){
            throw new CustomException(ExceptionCode.FAILED_TO_SAVE_FILE_IN_DIR);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //create thumbnail
        Tika tika = new Tika();

        String mimeType;
        try {
            mimeType = tika.detect(saveFile);
        } catch (IOException e) {
            throw new CustomException(ExceptionCode.FILE_MISSING);
        }

        if(mimeType.startsWith("image")){
            String thumbnailSaveName = thumbnailPath+"s_"+fileName+".jpeg";
            File thumbnailFile;
            File originalImage = saveFile;

            if(fileExtension.endsWith("svg") || fileExtension.endsWith("SVG")){
                thumbnailSaveName = thumbnailPath+"s_"+fileName+".svg";
                thumbnailFile = new File(thumbnailSaveName);
                try {
                    Files.copy(saveFile.toPath(), thumbnailFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new CustomException(ExceptionCode.FAILED_TO_CREATE_THUMBNAIL);
                }
            }
            else{
                thumbnailFile = new File(thumbnailSaveName);
                try {

                    Thumbnailator.createThumbnail(originalImage, thumbnailFile, 100, 100);
                } catch (IOException e){
                    throw new CustomException(ExceptionCode.FAILED_TO_CREATE_THUMBNAIL);
                } catch (Exception e){
                    e.printStackTrace();
                    throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
                }
            }
        }
        else if(mimeType.startsWith("video")){
            //generate thumbnail
            String thumbnailSaveName = thumbnailPath+"s_"+fileName+".jpeg";
            File thumbnailSaveFile = new File(thumbnailSaveName);

            try {
                fFmpegConfig.generateThumbnail(filePath, thumbnailSaveName);
                Thumbnailator.createThumbnail(thumbnailSaveFile, thumbnailSaveFile, 100, 100);
            } catch (IllegalArgumentException e){
                throw new CustomException(ExceptionCode.FAILED_TO_GENERATE_THUMBNAIL);
            } catch (IOException e) {
                e.printStackTrace();
                throw new CustomException(ExceptionCode.FAILED_TO_GENERATE_THUMBNAIL);
            } catch (Exception e){
                e.printStackTrace();
                throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
            }


            isStreaming = true;
            //hls convert
            String hlsPath = generatePath(path+"."+fileName);
            fFmpegConfig.convertToHlsVideo(filePath, hlsPath); // <- async
        }
        else if(mimeType.startsWith("audio")){

            String thumbnailSaveName = thumbnailPath+"s_"+fileName+".jpeg";
            File thumbnailSaveFile = new File(thumbnailSaveName);

            try{
                fFmpegConfig.getAlbumCoverImage(filePath, thumbnailSaveName);
                Thumbnailator.createThumbnail(thumbnailSaveFile, thumbnailSaveFile, 100, 100);
            } catch (Exception e){
                e.printStackTrace();
                throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
            }

            isStreaming = true;
            //hls convert
            String hlsPath = generatePath(path+"."+fileName);
            fFmpegConfig.convertToHlsAudio(filePath, hlsPath); // <- async
        }

        UserFile fileEntity = UserFile.builder()
                .userInfo(userInfo)
                .originalFileName(file.getOriginalFilename())
                .fileName(fileName)
                .fileExtension(fileExtension)
                .fileSize(file.getSize())
                .url(path)
                .isStreaming(isStreaming)
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

    private String generatePath(String path){
        File folderPath = new File(path);
        if(!folderPath.exists()){
            try{
                folderPath.mkdirs();
            } catch (Exception e){
                e.printStackTrace();
                throw new CustomException(ExceptionCode.FAILED_TO_MAKE_DIR);
            }
        }
        return folderPath.getPath()+"/";
    }

    private String generateSaveFileName(){
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        return uuid;
    }
}
