package com.seohamin.fshs.v2.domain.file.dto;

import com.seohamin.fshs.v2.domain.file.entity.Status;

public record FileStatusResponseDto(
        Status status,
        Long id
) { }
