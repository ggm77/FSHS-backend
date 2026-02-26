package com.seohamin.fshs.v2.domain.file.dto;

import org.springframework.core.io.Resource;

public record FileDownloadResponseDto(
        String name,
        String mimeType,
        Long size,
        Resource resource
) { }
