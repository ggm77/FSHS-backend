package org.iptime.raspinas.FSHS.service.userInfo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.config.WebSecurityConfig;
import org.iptime.raspinas.FSHS.dto.userInfo.request.UserInfoRequestDto;
import org.iptime.raspinas.FSHS.dto.userInfo.response.UserInfoResponseDto;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.repository.UserInfoRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.sql.SQLNonTransientConnectionException;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;
    private final WebSecurityConfig webSecurityConfig;

    public UserInfoResponseDto getUserInfo(Long id){
        try {
            UserInfo userInfo = userInfoRepository.findById(id).get();
            return UserInfoResponseDto.builder().userInfo(userInfo).build();
        } catch (NoSuchElementException e) {
            throw new CustomException(ExceptionCode.USER_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }
    @Transactional
    public UserInfoResponseDto updateUserInfo(UserInfoRequestDto requestDto, Long id){
        UserInfo user;
        try{
            user = userInfoRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            throw new CustomException(ExceptionCode.USER_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        if(requestDto.getUserEmail() != null && !requestDto.getUserEmail().isEmpty()){ // set user email
            boolean isExist;
            try{
                isExist = userInfoRepository.existsByUserEmail(requestDto.getUserEmail());
            } catch (DataAccessResourceFailureException e){
                throw new CustomException(ExceptionCode.DATABASE_DOWN);
            } catch (Exception e){
                e.printStackTrace();
                throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
            }

            if(isExist){
                throw new CustomException(ExceptionCode.EMAIL_DUPLICATE);
            }

            user.setUserEmail(requestDto.getUserEmail());
        }

        if(requestDto.getUserName() != null && !requestDto.getUserName().isEmpty()){
            user.setUserName(requestDto.getUserName());
        }

        if(requestDto.getUserPassword() != null && !requestDto.getUserPassword().isEmpty()){
            String encodePassword = webSecurityConfig.getPasswordEncoder().encode(requestDto.getUserPassword());
            user.setUserPassword(encodePassword);
        }

        if(requestDto.getUserProfilePictureUrl() != null && !requestDto.getUserProfilePictureUrl().isEmpty()){
            user.setUserProfilePictureUrl(requestDto.getUserProfilePictureUrl());
        }

        return UserInfoResponseDto.builder().userInfo(user).build();
    }

    public void deleteUserInfo(Long id){
        try{
            userInfoRepository.deleteById(id);
        } catch (NoSuchElementException e) {
            throw new CustomException(ExceptionCode.USER_ID_NOT_EXIST);
        } catch (DataAccessResourceFailureException e){
            throw new CustomException(ExceptionCode.DATABASE_DOWN);
        } catch (Exception e){
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }
}
