package com.seohamin.fshs.v2.global.infra.metadataExtractor.dto;

import java.time.Instant;

public record MetadataAnalysisResultDto(
        Instant capturedAt,
        Integer orientation,
        Double lat,
        Double lon,
        Integer width,
        Integer height
) { }
