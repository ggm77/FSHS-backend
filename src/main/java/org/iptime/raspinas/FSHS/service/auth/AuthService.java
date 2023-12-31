package org.iptime.raspinas.FSHS.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.config.WebSecurityConfig;
import org.iptime.raspinas.FSHS.dto.auth.signIn.reqeust.SignInRequestDto;
import org.iptime.raspinas.FSHS.dto.auth.signIn.response.SignInResponseDto;
import org.iptime.raspinas.FSHS.dto.auth.signUp.request.SignUpRequestDto;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.userInfo.UserInfoRepository;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

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

        try {
            return userInfoRepository.save(userInfo);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("AuthService.signUp message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
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
