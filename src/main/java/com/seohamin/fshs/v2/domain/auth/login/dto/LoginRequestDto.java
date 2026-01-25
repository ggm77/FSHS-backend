package com.seohamin.fshs.v2.domain.auth.login.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoginRequestDto {

    @NotNull
    private String username;

    @NotNull
    private String password;
}
