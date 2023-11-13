package org.iptime.raspinas.FSHS.service.auth;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.config.WebSecurityConfig;
import org.iptime.raspinas.FSHS.dto.auth.signIn.reqeust.SignInRequestDto;
import org.iptime.raspinas.FSHS.dto.auth.signIn.response.SignInResponseDto;
import org.iptime.raspinas.FSHS.dto.auth.signUp.request.SignUpRequestDto;
import org.iptime.raspinas.FSHS.entity.user.UserInfo;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.UserInfoRepository;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserInfoRepository userInfoRepository;
    private final TokenProvider tokenProvider;
    private final WebSecurityConfig webSecurityConfig;

    public UserInfo signUp(SignUpRequestDto requestDto){
        String userName = requestDto.getUserName();
        String userEmail = requestDto.getUserEmail();
        String userPassword = webSecurityConfig.getPasswordEncoder().encode(requestDto.getUserPassword());
        SignUpRequestDto putDto = new SignUpRequestDto(userName, userEmail, userPassword);
        boolean isEmailExist = false;
        try {
            isEmailExist = userInfoRepository.existsByUserEmail(userEmail);
        } catch (Exception e){
            e.printStackTrace();
        }

        if(isEmailExist) {
            throw new CustomException(ExceptionCode.EMAIL_DUPLICATE);
        }

        UserInfo userInfo = new UserInfo(putDto);

        try{
            UserInfo result = userInfoRepository.save(userInfo);
            return result;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public SignInResponseDto signIn(SignInRequestDto requestDto){
        String userEmail = requestDto.getUserEmail();
        UserInfo userInfo = userInfoRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
        boolean isPasswordMatch = webSecurityConfig.getPasswordEncoder().matches(requestDto.getUserPassword(), userInfo.getUserPassword());

        if(!isPasswordMatch){
            throw new CustomException(ExceptionCode.PASSWORD_NOT_MATCHED);
        }
        userInfo.setUserPassword("");

        String accessToken = tokenProvider.createAccessToken(userInfo.getId());
        String refreshToken = tokenProvider.createRefreshToken(userInfo.getId());

        int exprTime = 360000;
        String tokenType = tokenProvider.getTokenType();

        return SignInResponseDto.builder()
                .userInfo(userInfo)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .exprTime(exprTime)
                .tokenType(tokenType).build();
    }
}
