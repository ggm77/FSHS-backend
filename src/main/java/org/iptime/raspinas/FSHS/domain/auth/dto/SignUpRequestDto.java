package org.iptime.raspinas.FSHS.domain.auth.dto;

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
