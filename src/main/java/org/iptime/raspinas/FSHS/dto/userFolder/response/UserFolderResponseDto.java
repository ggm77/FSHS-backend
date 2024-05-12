package org.iptime.raspinas.FSHS.dto.userFolder.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class UserFolderResponseDto {
    private Long id;
    private String originalFileName;
    private String fileName;
    private Long fileSize;
    private String fileExtension;
    private Timestamp uploadDate;
    private Timestamp updateDate;
    private String url;
    private boolean isDirectory;
    private boolean isFavorite;
    private boolean hasThumbnail;
    private boolean isStreaming;
    private boolean isStreamingMusic;
    private boolean isStreamingVideo;
    private boolean isShared;
    private boolean isSecrete;
    private boolean isDeleted;
    private Timestamp deleteDate;
    private List<UserFolderResponseDto> children;

    public static UserFolderResponseDto fromEntity(UserFile file) {
        UserFolderResponseDto dto = new UserFolderResponseDto();
        dto.setId(file.getId());
        dto.setOriginalFileName(file.getOriginalFileName());
        dto.setFileName(file.getFileName());
        dto.setFileSize(file.getFileSize());
        dto.setFileExtension(file.getFileExtension());
        dto.setUploadDate(file.getUploadDate());
        dto.setUpdateDate(file.getUpdateDate());
        dto.setUrl(file.getUrl());
        dto.setDirectory(file.isDirectory());
        dto.setFavorite(file.isFavorite());
        dto.setHasThumbnail(file.isHasThumbnail());
        dto.setStreaming(file.isStreaming());
        dto.setStreamingMusic(file.isStreamingMusic());
        dto.setStreamingVideo(file.isStreamingVideo());
        dto.setShared(file.isShared());
        dto.setSecrete(file.isSecrete());
        dto.setDeleted(file.isDeleted());
        dto.setDeleteDate(file.getDeletedDate());
        if (file.getChildren() != null) {
            dto.setChildren(file.getChildren().stream()
                    .map(UserFolderResponseDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
