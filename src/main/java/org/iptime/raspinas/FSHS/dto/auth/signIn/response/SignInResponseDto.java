package org.iptime.raspinas.FSHS.dto.auth.signIn.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userFile.response.UserFileSimpleResponseDto;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInResponseDto {
    private String accessToken;
    private String tokenType;
    private int exprTime;
    private String refreshToken;
    private Long id;
    private String userName;
    private String userEmail;
    private String userProfilePictureUrl;
    private Timestamp signUpDate;
    private boolean isAdmin;
    private boolean isDisabled;

    @Builder
    public SignInResponseDto(
            final UserInfo userInfo,
            final String accessToken,
            final String refreshToken,
            final String tokenType,
            final int exprTime
    ){
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.exprTime = exprTime;
        this.refreshToken = refreshToken;
        this.id = userInfo.getId();
        this.userName = userInfo.getUserName();
        this.userEmail = userInfo.getUserEmail();
        this.userProfilePictureUrl = userInfo.getUserProfilePictureUrl();
        this.signUpDate = userInfo.getSignUpDate();
        this.isAdmin = userInfo.isAdmin();
        this.isDisabled = userInfo.isDisabled();
    }
}
