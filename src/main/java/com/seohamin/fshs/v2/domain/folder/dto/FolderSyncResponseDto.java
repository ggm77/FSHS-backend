package com.seohamin.fshs.v2.domain.folder.dto;

import java.util.List;

public record FolderSyncResponseDto(
        List<String> createdFolders,
        List<String> createdFiles,
        List<String> updatedFiles,
        List<String> deletedFolders,
        List<String> deletedFiles,
        List<String> skipped,
        List<String> errors
) {
}
