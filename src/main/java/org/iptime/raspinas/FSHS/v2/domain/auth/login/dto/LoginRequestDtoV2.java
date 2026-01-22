package org.iptime.raspinas.FSHS.v2.domain.auth.login.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoginRequestDtoV2 {

    @NotNull
    private String username;

    @NotNull
    private String password;
}
