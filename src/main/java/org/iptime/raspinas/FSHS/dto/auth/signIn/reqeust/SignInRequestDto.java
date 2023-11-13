package org.iptime.raspinas.FSHS.dto.auth.signIn.reqeust;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInRequestDto {
    private String userEmail;
    private String userPassword;
}
