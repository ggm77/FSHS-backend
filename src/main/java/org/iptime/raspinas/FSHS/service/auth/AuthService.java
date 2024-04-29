package org.iptime.raspinas.FSHS.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.config.WebSecurityConfig;
import org.iptime.raspinas.FSHS.dto.auth.signIn.reqeust.SignInRequestDto;
import org.iptime.raspinas.FSHS.dto.auth.signIn.response.SignInResponseDto;
import org.iptime.raspinas.FSHS.dto.auth.signUp.request.SignUpRequestDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.userFile.UserFileRepository;
import org.iptime.raspinas.FSHS.repository.userInfo.UserInfoRepository;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    private final UserFileRepository userFileRepository;
    private final UserInfoRepository userInfoRepository;
    private final TokenProvider tokenProvider;
    private final WebSecurityConfig webSecurityConfig;

    public UserInfo signUp(final SignUpRequestDto requestDto){

        final String userName = requestDto.getUserName();
        final String userEmail = requestDto.getUserEmail();
        final String userPassword = webSecurityConfig.getPasswordEncoder().encode(requestDto.getUserPassword());
        final SignUpRequestDto putDto = new SignUpRequestDto(userName, userEmail, userPassword);

        boolean isEmailExist;
        try {
            isEmailExist = userInfoRepository.existsByUserEmail(userEmail);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("AuthService.signUp message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //Prevent duplicate email registrations. | 이메일 중복 방지
        if(isEmailExist) {
            throw new CustomException(ExceptionCode.EMAIL_DUPLICATE);
        }

        final UserInfo userInfo = new UserInfo(putDto);
        final UserInfo result;

        try {
            result = userInfoRepository.save(userInfo);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("AuthService.signUp message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }


        final String userDirPath = UserFileDirPath + "/" + result.getId().toString();
        final String userThumbnailDirPath = UserFileDirPath + "/thumbnail/" + result.getId().toString();

        try {
            final Path path = Paths.get(userDirPath);
            final Path thumbnailPath = Paths.get(userThumbnailDirPath);
            if(!Files.exists(path)){
                Files.createDirectories(path);
            }
            if(!Files.exists(thumbnailPath)){
                Files.createDirectories(thumbnailPath);
            }
        } catch (IOException ex){
            log.error("AuthService.signUp message:{}",ex.getMessage(), ex);
            throw new CustomException(ExceptionCode.FAILED_TO_MAKE_DIR);
        }

        final UserFile root = UserFile.builder()
                .userInfo(result)
                .originalFileName(result.getId().toString())
                .fileName(result.getId().toString())
                .url("/" + result.getId().toString())
                .isDirectory(true)
                .isStreaming(false)
                .isSecrete(false)
                .build();

        try {
            userFileRepository.save(root);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("AuthService.signUp message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    public SignInResponseDto signIn(final SignInRequestDto requestDto){

        final String userEmail = requestDto.getUserEmail();

        final UserInfo userInfo;
        try{
            userInfo = userInfoRepository.findByUserEmail(userEmail).get();
        } catch (NoSuchElementException ex){
            throw new CustomException(ExceptionCode.USER_EMAIL_NOT_EXIST);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("AuthService.signIn message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        final boolean isPasswordMatch = webSecurityConfig.getPasswordEncoder().matches(
                requestDto.getUserPassword(),
                userInfo.getUserPassword()
        );

        //Password validation. | 비밀번호 체크
        if(!isPasswordMatch){
            throw new CustomException(ExceptionCode.PASSWORD_NOT_MATCHED);
        }
        userInfo.setUserPassword("");

        final String accessToken = tokenProvider.createAccessToken(userInfo.getId());
        final String refreshToken = tokenProvider.createRefreshToken(userInfo.getId());
        final int exprTime = 360000;
        final String tokenType = tokenProvider.getTokenType();

        return SignInResponseDto.builder()
                .userInfo(userInfo)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .exprTime(exprTime)
                .tokenType(tokenType).build();
    }
}
