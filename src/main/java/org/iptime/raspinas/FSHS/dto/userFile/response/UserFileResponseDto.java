package org.iptime.raspinas.FSHS.dto.userFile.response;

import lombok.Getter;
import org.iptime.raspinas.FSHS.dto.userFolder.response.UserFolderResponseDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class UserFileResponseDto {

    private Long id;
    private Long userId;
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
    private Timestamp deletedDate;
    private List<UserFileSimpleResponseDto> children;

    public UserFileResponseDto(final UserFile userFile){
        this.id = userFile.getId();
        this.userId = userFile.getUserInfo().getId();
        this.originalFileName = userFile.getOriginalFileName();
        this.fileName = userFile.getFileName();
        this.fileSize = userFile.getFileSize();
        this.fileExtension = userFile.getFileExtension();
        this.uploadDate = userFile.getUploadDate();
        this.updateDate = userFile.getUpdateDate();
        this.url = userFile.getUrl();
        this.isDirectory = userFile.isDirectory();
        this.isFavorite = userFile.isFavorite();
        this.hasThumbnail = userFile.isHasThumbnail();
        this.isStreaming = userFile.isStreaming();
        this.isStreamingMusic = userFile.isStreamingMusic();
        this.isStreamingVideo = userFile.isStreamingVideo();
        this.isShared = userFile.isShared();
        this.isSecrete = userFile.isSecrete();
        this.isDeleted = userFile.isDeleted();
        this.deletedDate = userFile.getDeletedDate();
        if (userFile.getChildren() != null) {
            this.children = userFile.getChildren().stream()
                    .map(UserFileSimpleResponseDto::new)
                    .collect(Collectors.toList());
        }
    }
}
