package com.seohamin.fshs.v2.domain.folder.dto;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record FolderDownloadResponseDto(String name, StreamingResponseBody stream) {}