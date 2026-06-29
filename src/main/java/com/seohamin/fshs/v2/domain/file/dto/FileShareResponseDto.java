package com.seohamin.fshs.v2.domain.file.dto;

public record FileShareResponseDto(
        Long id,
        Long fileId,
        String shareKey
) {
}
