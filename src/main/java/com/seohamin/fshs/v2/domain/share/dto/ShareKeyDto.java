package com.seohamin.fshs.v2.domain.share.dto;

import com.seohamin.fshs.v2.domain.share.entity.SharedFile;

public record ShareKeyDto(
        Long id,
        Long fileId,
        String shareKey
) {

    public static ShareKeyDto of(final SharedFile sharedFile) {
        return new ShareKeyDto(
                sharedFile.getId(),
                sharedFile.getFile().getId(),
                sharedFile.getShareKey()
        );
    }
}
