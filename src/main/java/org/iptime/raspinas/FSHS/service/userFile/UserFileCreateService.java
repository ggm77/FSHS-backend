package org.iptime.raspinas.FSHS.service.userFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class UserFileCreateService {

    private final UserFileRepository userFileRepository;
    private final UserInfoRepository userInfoRepository;
    private final FFmpegConfig fFmpegConfig;

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    public List<UserFile> createUserFile(
            final List<MultipartFile> files,
            final UserFileCreateRequestDto requestDto,
            final Long id
    ){

        //Exclude when the file does not exist. | 파일 없을 때 제외
        if(files.isEmpty()){
            throw new CustomException(ExceptionCode.FILE_NOT_UPLOADED);
        }

        //Validate the correctness of the provided file path. | 경로가 올바른지 체크
        if(!requestDto.getPath().startsWith("/") || !requestDto.getPath().endsWith("/")){
            throw new CustomException(ExceptionCode.PATH_NOT_VALID);
        }

        final List<UserFile> result = new ArrayList<>();

        final UserInfo userInfo = userInfoRepository.findById(id).get();
        final String filePath = generatePath("/"+id+requestDto.getPath()); // input : ' /{folderName}/{folderName}/ '   -->>  ' /userId/{folderName}/{folderName}/ '
        final String thumbnailPath = generatePath("/thumbnail/"+id+requestDto.getPath());
        final boolean isSecrete = requestDto.isSecrete();

        //Process multiple files. | 복수의 파일 처리
        for(MultipartFile multipartFile : files){

            //Exclude when the file does not exist. | 파일 없을 때 제외
            if(multipartFile.isEmpty()){
                continue;
            }

            result.add(saveFile(multipartFile, filePath, thumbnailPath, userInfo, isSecrete));
        }

        return result;
    }


    private UserFile saveFile(
            final MultipartFile file,
            final String path,
            final String thumbnailPath,
            final UserInfo userInfo,
            final boolean isSecrete){

        final String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        final String fileName = generateSaveFileName();//uuid
        final String filePath = UserFileDirPath+path+fileName+"."+fileExtension;

        boolean isStreaming = false;

        final File saveFile = new File(filePath);

        try { // saving file
            file.transferTo(saveFile);
        } catch (IOException ex){
            throw new CustomException(ExceptionCode.FAILED_TO_SAVE_FILE_IN_DIR);
        } catch (Exception ex){
            log.error("UserFileCreateService.saveFile message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //create thumbnail
        final Tika tika = new Tika();

        final String mimeType;
        try {
            mimeType = tika.detect(saveFile);
        } catch (IOException ex) {
            throw new CustomException(ExceptionCode.FILE_MISSING);
        }

        //for image files | 이미지 파일인 경우
        if(mimeType.startsWith("image")){

            final File thumbnailFile;
            final File originalImage = saveFile;

            //Handle SVG file processing. | svg 파일 예외 처리
            if(fileExtension.endsWith("svg") || fileExtension.endsWith("SVG")){

                final String thumbnailSaveName = UserFileDirPath+thumbnailPath+"s_"+fileName+".svg";
                thumbnailFile = new File(thumbnailSaveName);

                try {
                    Files.copy(saveFile.toPath(), thumbnailFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException ex) {
                    log.error("UserFileCreateService.saveFile message:{}",ex.getMessage(),ex);
                    throw new CustomException(ExceptionCode.FAILED_TO_CREATE_THUMBNAIL);
                }
            }
            else{

                final String thumbnailSaveName = UserFileDirPath+thumbnailPath+"s_"+fileName+".jpeg";
                thumbnailFile = new File(thumbnailSaveName);

                try {

                    Thumbnailator.createThumbnail(originalImage, thumbnailFile, 100, 100);
                } catch (IOException ex){
                    throw new CustomException(ExceptionCode.FAILED_TO_CREATE_THUMBNAIL);
                } catch (Exception ex){
                    log.error("UserFileCreateService.saveFile message:{}",ex.getMessage(),ex);
                    throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
                }
            }
        }

        //for video files | 비디오 파일인 경우
        else if(mimeType.startsWith("video")){
            //generate thumbnail
            final String thumbnailSaveName = UserFileDirPath+thumbnailPath+"s_"+fileName+".jpeg";
            final File thumbnailSaveFile = new File(thumbnailSaveName);

            try {
                fFmpegConfig.generateThumbnail(filePath, thumbnailSaveName);
                Thumbnailator.createThumbnail(thumbnailSaveFile, thumbnailSaveFile, 100, 100);
            } catch (IllegalArgumentException ex){
                throw new CustomException(ExceptionCode.FAILED_TO_GENERATE_THUMBNAIL);
            } catch (IOException ex) {
                log.error("UserFileCreateService.saveFile message:{}",ex.getMessage(),ex);
                throw new CustomException(ExceptionCode.FAILED_TO_GENERATE_THUMBNAIL);
            } catch (Exception ex){
                log.error("UserFileCreateService.saveFile message:{}",ex.getMessage(),ex);
                throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
            }


            isStreaming = true;
            //hls convert
            final String hlsPath = UserFileDirPath+generatePath(path+"."+fileName);
            fFmpegConfig.convertToHlsVideo(filePath, hlsPath); // <- async
        }

        //for audio files | 오디오 파일인 경우
        else if(mimeType.startsWith("audio")){

            final String thumbnailSaveName = UserFileDirPath+thumbnailPath+"s_"+fileName+".jpeg";
            final File thumbnailSaveFile = new File(thumbnailSaveName);

            try{
                fFmpegConfig.getAlbumCoverImage(filePath, thumbnailSaveName);
                Thumbnailator.createThumbnail(thumbnailSaveFile, thumbnailSaveFile, 100, 100);
            } catch (Exception ex){
                log.error("UserFileCreateService.saveFile message:{}",ex.getMessage(),ex);
                throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
            }

            isStreaming = true;
            //hls convert
            final String hlsPath = UserFileDirPath+generatePath(path+"."+fileName);
            fFmpegConfig.convertToHlsAudio(filePath, hlsPath); // <- async
        }

        final UserFile fileEntity = UserFile.builder()
                .userInfo(userInfo)
                .originalFileName(file.getOriginalFilename())
                .fileName(fileName)
                .fileExtension(fileExtension)
                .fileSize(file.getSize())
                .url(path)
                .isStreaming(isStreaming)
                .isSecrete(isSecrete)
                .build();

        final UserFile result;
        try{
            result = userFileRepository.save(fileEntity);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserFileCreateService.saveFile message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    private String generatePath(final String path){ //use relative path
        final File folderPath = new File(UserFileDirPath+path);

        //Handle the case when the folder does not exist. | 폴더 위치가 존재하지 않을 때
        if(!folderPath.exists()){
            try{
                folderPath.mkdirs();
            } catch (Exception ex){
                log.error("UserFileCreateService.generatePath message:{}",ex.getMessage(),ex);
                throw new CustomException(ExceptionCode.FAILED_TO_MAKE_DIR);
            }
        }
        return path;
    }

    private String generateSaveFileName(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }
}
