package org.iptime.raspinas.FSHS.dto.auth.signUp.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDto {
    private String userName;
    private String userEmail;
    private String userPassword;
}
