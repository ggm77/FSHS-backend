package com.seohamin.fshs.v2.domain.file.dto;

import com.seohamin.fshs.v2.global.validation.Create;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record FileRequestDto (

    @NotNull(groups = Create.class)
    Long folderId,

    @NotNull(groups = Create.class)
    Instant originCreatedAt,

    @NotNull(groups = Create.class)
    Instant originUpdatedAt
) {}
