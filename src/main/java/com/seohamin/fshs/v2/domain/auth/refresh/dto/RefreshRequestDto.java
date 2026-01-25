package com.seohamin.fshs.v2.domain.auth.refresh.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RefreshRequestDto {
    @NotNull
    private String refreshToken;
}
