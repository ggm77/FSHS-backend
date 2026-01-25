package com.seohamin.fshs.v2.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import com.seohamin.fshs.v2.global.validation.Create;

@Getter
public class UserRequestDto {

    @NotNull(groups = Create.class)
    public String username;

    @NotNull(groups = Create.class)
    public String password;
}
