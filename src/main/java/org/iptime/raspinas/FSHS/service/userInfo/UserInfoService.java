package org.iptime.raspinas.FSHS.service.userInfo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.config.WebSecurityConfig;
import org.iptime.raspinas.FSHS.dto.userInfo.request.UserInfoRequestDto;
import org.iptime.raspinas.FSHS.dto.userInfo.response.UserInfoResponseDto;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.userInfo.UserInfoRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;
    private final WebSecurityConfig webSecurityConfig;

    public UserInfoResponseDto getUserInfo(final Long id){

        try {
            final UserInfo userInfo = userInfoRepository.findById(id).get();
            return UserInfoResponseDto.builder().userInfo(userInfo).build();
        } catch (NoSuchElementException ex) {
            throw new CustomException(ExceptionCode.USER_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserInfoService.getUserInfo message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public UserInfoResponseDto updateUserInfo(
            final UserInfoRequestDto requestDto,
            final Long id){

        final UserInfo user;
        try{
            user = userInfoRepository.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new CustomException(ExceptionCode.USER_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserInfoService.updateUserInfo message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        //Update when the email is not empty. | 이메일이 비어있지 않다면 업데이트
        if(requestDto.getUserEmail() != null && !requestDto.getUserEmail().isEmpty()){ // set user email
            boolean isExist;
            try{
                isExist = userInfoRepository.existsByUserEmail(requestDto.getUserEmail());
            } catch (DataAccessResourceFailureException ex){
                throw new CustomException(ExceptionCode.DATABASE_DOWN);
            } catch (Exception ex){
                log.error("UserInfoService.updateUserInfo message:{}",ex.getMessage(),ex);
                throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
            }

            //Prevent duplicate email registrations. | 이메일 중복 방지
            if(isExist){
                throw new CustomException(ExceptionCode.EMAIL_DUPLICATE);
            }

            user.setUserEmail(requestDto.getUserEmail());
        }

        //Update when the user name is not empty. | 유저 이름이 비어있지 않다면 업데이트
        if(requestDto.getUserName() != null && !requestDto.getUserName().isEmpty()){
            user.setUserName(requestDto.getUserName());
        }

        //Update when the password is not empty. | 비밀번호가 비어있지 않다면 업데이트
        if(requestDto.getUserPassword() != null && !requestDto.getUserPassword().isEmpty()){
            final String encodePassword = webSecurityConfig.getPasswordEncoder().encode(requestDto.getUserPassword());
            user.setUserPassword(encodePassword);
        }

        //Update when the profile picture is not empty. | 프로필 사진이 비어있지 않다면 업데이트
        if(requestDto.getUserProfilePictureUrl() != null && !requestDto.getUserProfilePictureUrl().isEmpty()){
            user.setUserProfilePictureUrl(requestDto.getUserProfilePictureUrl());
        }

        return UserInfoResponseDto.builder().userInfo(user).build();
    }

    public void deleteUserInfo(final Long id){

        try{
            userInfoRepository.deleteById(id);
        } catch (NoSuchElementException ex) {
            throw new CustomException(ExceptionCode.USER_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException ex){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception ex){
            log.error("UserInfoService.deleteUserInfo message:{}",ex.getMessage(),ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }
}
