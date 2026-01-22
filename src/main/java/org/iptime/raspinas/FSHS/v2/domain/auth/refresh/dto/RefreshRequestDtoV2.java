package org.iptime.raspinas.FSHS.v2.domain.auth.refresh.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RefreshRequestDtoV2 {
    @NotNull
    private String refreshToken;
}
