package org.iptime.raspinas.FSHS.service.auth;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.config.WebSecurityConfig;
import org.iptime.raspinas.FSHS.dto.auth.signUp.request.SignUpRequestDto;
import org.iptime.raspinas.FSHS.dto.auth.signUp.response.SignUpResponseDto;
import org.iptime.raspinas.FSHS.entity.user.UserInfo;
import org.iptime.raspinas.FSHS.repository.UserInfoRepository;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserInfoRepository userInfoRepository;
    private final TokenProvider tokenProvider;
    private final WebSecurityConfig webSecurityConfig;

    public SignUpResponseDto signUp(SignUpRequestDto requestDto){
        String userName = requestDto.getUserName();
        String userEmail = requestDto.getUserEmail();
        String userPassword = webSecurityConfig.getPasswordEncoder().encode(requestDto.getUserPassword());
        SignUpRequestDto putDto = new SignUpRequestDto(userName, userEmail, userPassword);
        try{
            if(userInfoRepository.existsByUserEmail(userEmail)) {
                throw new IllegalArgumentException("Email already exist in DB.");
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        UserInfo userInfo = new UserInfo(putDto);

        try{
            userInfoRepository.save(userInfo);
        } catch (Exception e){
            e.printStackTrace();
        }

        return new SignUpResponseDto(true, "Successfully sign up. Please sign in");
    }
}
