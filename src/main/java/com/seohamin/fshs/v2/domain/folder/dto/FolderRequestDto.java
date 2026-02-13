package com.seohamin.fshs.v2.domain.folder.dto;

public record FolderRequestDto(
        Long parentFolderId,
        String name,
        Boolean isRoot
) { }
