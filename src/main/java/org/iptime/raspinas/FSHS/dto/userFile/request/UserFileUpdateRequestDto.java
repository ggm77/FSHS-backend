package org.iptime.raspinas.FSHS.dto.userFile.request;

import lombok.Getter;

@Getter
public class UserFileUpdateRequestDto {
    private String originalFileName;
    private Boolean isFavorite;
    private Boolean isStreaming;
    private Boolean isStreamingMusic;
    private Boolean isStreamingVideo;
    private Boolean isShared;
    private Boolean isSecrete;
}
