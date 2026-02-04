package com.seohamin.fshs.v2.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import com.seohamin.fshs.v2.global.validation.Create;

public record UserRequestDto(

    @NotNull(groups = Create.class)
    String username,

    @NotNull(groups = Create.class)
    String password
) {}
