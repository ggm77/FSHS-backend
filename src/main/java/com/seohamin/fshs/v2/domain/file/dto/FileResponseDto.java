package com.seohamin.fshs.v2.domain.file.dto;

import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;

import java.time.Instant;
import java.util.List;

public record FileResponseDto (
        Long id,
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
        Instant originCreatedAt,
        Instant originUpdatedAt,
        Instant createdAt,
        Instant updatedAt,
        String category,
        Boolean isNfd,
        Boolean isShared,
        List<String> shareKeys
) {
    public static FileResponseDto of(final File file) {
        return new FileResponseDto(
                file.getId(),
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
                file.getOriginCreatedAt(),
                file.getOriginUpdatedAt(),
                file.getCreatedAt(),
                file.getUpdatedAt(),
                file.getCategory().name(),
                file.getIsNfd(),
                !file.getSharedFiles().isEmpty(),
                file.getSharedFiles().stream()
                        .map(SharedFile::getShareKey)
                        .toList()
        );
    }
}
