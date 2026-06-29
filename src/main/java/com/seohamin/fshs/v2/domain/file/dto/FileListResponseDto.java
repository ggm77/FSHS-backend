package com.seohamin.fshs.v2.domain.file.dto;

import java.util.List;

public record FileListResponseDto(
        boolean hasNext,
        List<FileResponseDto> items
) {
}
