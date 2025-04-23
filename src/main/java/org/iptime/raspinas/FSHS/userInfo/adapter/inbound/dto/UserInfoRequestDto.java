package org.iptime.raspinas.FSHS.userInfo.adapter.inbound.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoRequestDto {
    private String userName;
    private String userEmail;
    private String userPassword;
    private String userProfilePictureUrl;
}
