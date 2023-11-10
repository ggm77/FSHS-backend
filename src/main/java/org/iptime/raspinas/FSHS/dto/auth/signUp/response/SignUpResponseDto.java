package org.iptime.raspinas.FSHS.dto.auth.signUp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponseDto {
    private boolean result;
    private String message;
}
