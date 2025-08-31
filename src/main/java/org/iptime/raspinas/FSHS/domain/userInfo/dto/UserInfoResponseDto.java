package org.iptime.raspinas.FSHS.domain.userInfo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.iptime.raspinas.FSHS.domain.userInfo.domain.UserInfo;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {
    private Long id;
    private String userName;
    private String userEmail;
    private String userProfilePictureUrl;
    private Timestamp signUpDate;
    private boolean isAdmin;
    private boolean isDisabled;

    @Builder
    public UserInfoResponseDto(final UserInfo userInfo){
        this.id = userInfo.getId();
        this.userName = userInfo.getUserName();
        this.userEmail = userInfo.getUserEmail();
        this.userProfilePictureUrl = userInfo.getUserProfilePictureUrl();
        this.signUpDate = userInfo.getSignUpDate();
        this.isAdmin = userInfo.isAdmin();
        this.isDisabled = userInfo.isDisabled();
    }
}
