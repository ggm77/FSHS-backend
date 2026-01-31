package com.seohamin.fshs.v2.domain.auth.account.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SignUpRequestDto {

    @NotNull
    private String username;

    @NotNull
    private String password;
}
