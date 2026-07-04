package com.seohamin.fshs.v2.domain.auth.file.dto;

import com.seohamin.fshs.v2.domain.file.entity.File;

import java.time.Instant;

public record SharedFileResponseDto (
        Long id,
        String uuid,
        Long ownerId,
        String name,
        String baseName,
        String extension,
        String relativePath,
        String parentPath,
        String mimeType,
        Long size,
        String videoCodec,
        String audioCodec,
        Integer width,
        Integer height,
        Long duration,
        Integer bitrate,
        Integer orientation,
        Double lat,
        Double lon,
        Double fps,
        String format,
        Instant capturedAt,
        Instant originUpdatedAt,
        Instant createdAt,
        Instant updatedAt,
        String category,
        Boolean isShared
) {
    public static SharedFileResponseDto of(final File file) {
        return new SharedFileResponseDto(
                file.getId(),
                file.getUuid(),
                file.getOwnerId(),
                file.getName(),
                file.getBaseName(),
                file.getExtension(),
                file.getRelativePath(),
                file.getParentPath(),
                file.getMimeType(),
                file.getSize(),
                file.getVideoCodec(),
                file.getAudioCodec(),
                file.getWidth(),
                file.getHeight(),
                file.getDuration(),
                file.getBitrate(),
                file.getOrientation(),
                file.getLat(),
                file.getLon(),
                file.getFps(),
                file.getFormat(),
                file.getCapturedAt(),
                file.getOriginUpdatedAt(),
                file.getCreatedAt(),
                file.getUpdatedAt(),
                file.getCategory().name(),
                !file.getSharedFiles().isEmpty()
        );
    }
}