package com.seohamin.fshs.v2.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import com.seohamin.fshs.v2.global.validation.Create;

@Getter
public class UserRequestDto {

    @NotNull(groups = Create.class)
    private String username;

    @NotNull(groups = Create.class)
    private String password;

    @Builder
    public UserRequestDto(
            final String username,
            final String password
    ) {
        this.username = username;
        this.password = password;
    }
}
