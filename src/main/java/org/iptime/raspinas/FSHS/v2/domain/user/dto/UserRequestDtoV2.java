package org.iptime.raspinas.FSHS.v2.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.iptime.raspinas.FSHS.v2.global.validation.Create;

@Getter
public class UserRequestDtoV2 {

    @NotNull(groups = Create.class)
    public String username;

    @NotNull(groups = Create.class)
    public String password;
}
