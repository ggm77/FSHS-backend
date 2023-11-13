package org.iptime.raspinas.FSHS.dto.auth.signIn.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.iptime.raspinas.FSHS.entity.user.UserFile;
import org.iptime.raspinas.FSHS.entity.user.UserInfo;

import java.sql.Timestamp;
import java.util.List;

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
    private List<UserFile> userFileList;
    private boolean isAdmin;
    private boolean isDisabled;

    @Builder
    public SignInResponseDto(UserInfo userInfo, String accessToken, String refreshToken, String tokenType, int exprTime){
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.exprTime = exprTime;
        this.refreshToken = refreshToken;
        this.id = userInfo.getId();
        this.userName = userInfo.getUserName();
        this.userEmail = userInfo.getUserEmail();
        this.userProfilePictureUrl = userInfo.getUserProfilePictureUrl();
        this.signUpDate = userInfo.getSignUpDate();
        this.userFileList = userInfo.getUserFile();
        this.isAdmin = userInfo.isAdmin();
        this.isDisabled = userInfo.isDisabled();
    }
}
