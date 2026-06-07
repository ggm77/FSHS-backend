package com.seohamin.fshs.v2.domain.file.dto;

import java.time.Instant;

public record FileUpdateRequestDto(
        Long folderId,
        Instant originUpdatedAt
) { }
