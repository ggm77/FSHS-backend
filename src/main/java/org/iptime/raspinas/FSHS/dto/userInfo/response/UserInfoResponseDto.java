package org.iptime.raspinas.FSHS.dto.userInfo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userFile.response.UserFileSimpleResponseDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {
    private Long id;
    private String userName;
    private String userEmail;
    private String userProfilePictureUrl;
    private Timestamp signUpDate;
    private List<UserFileSimpleResponseDto> userFile = new ArrayList<>();
    private boolean isAdmin;
    private boolean isDisabled;

    @Builder
    public UserInfoResponseDto(UserInfo userInfo){
        this.id = userInfo.getId();
        this.userName = userInfo.getUserName();
        this.userEmail = userInfo.getUserEmail();
        this.userProfilePictureUrl = userInfo.getUserProfilePictureUrl();
        this.signUpDate = userInfo.getSignUpDate();
        this.isAdmin = userInfo.isAdmin();
        this.isDisabled = userInfo.isDisabled();
        if(userInfo.getUserFile() != null){
            this.userFile = userInfo.getUserFile().stream()
                    .map(UserFileSimpleResponseDto :: new)
                    .collect(Collectors.toList());
        }
    }
}
