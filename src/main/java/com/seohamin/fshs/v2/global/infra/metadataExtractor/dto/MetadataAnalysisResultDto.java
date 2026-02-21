package com.seohamin.fshs.v2.global.infra.metadataExtractor.dto;

import java.time.Instant;

public record MetadataAnalysisResultDto(
        Instant capturedAt,
        Integer orientation,
        String latLon,
        Integer width,
        Integer height
) { }
