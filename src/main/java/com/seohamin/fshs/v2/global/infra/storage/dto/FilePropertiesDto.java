package com.seohamin.fshs.v2.global.infra.storage.dto;

import com.seohamin.fshs.v2.domain.file.entity.Category;

import java.time.Instant;

public record FilePropertiesDto(
        String mimeType,
        Long size,
        Category category,
        String name,
        String baseName,
        String extension
) { }
