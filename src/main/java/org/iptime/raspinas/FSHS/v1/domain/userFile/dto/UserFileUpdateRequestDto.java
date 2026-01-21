package org.iptime.raspinas.FSHS.v1.domain.userFile.dto;

import lombok.Getter;

@Getter
public class UserFileUpdateRequestDto {
    private String newFileName; //not include file extension
    private Boolean isFavorite;
    private Boolean hasThumbnail;
    private Boolean isStreaming;
    private Boolean isStreamingMusic;
    private Boolean isStreamingVideo;
    private Boolean isShared;
    private Boolean isSecrete;
}
