package com.seohamin.fshs.v2.domain.file.controller;

import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // 파일 단건 업로드 API
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponseDto> uploadFile(
            @RequestPart(value = "file") final MultipartFile multipartFile,
            @RequestParam(value = "lastModified") final Instant lastModified,
            @RequestParam(value = "folderId") final Long folderId
    ) {

        return ResponseEntity.ok().body(fileService.uploadFile(multipartFile, lastModified, folderId));
    }

    // 특정 파일 정보 조회 API
    @GetMapping("/files/{fileId}")
    public ResponseEntity<FileResponseDto> getFileDetails(
            @PathVariable final Long fileId
    ) {

        return ResponseEntity.ok().body(fileService.getFileDetails(fileId));
    }

    // 파일 다운로드 및 직접 스트리밍 API
    @GetMapping("/files/{fileId}/content")
    public ResponseEntity<Resource> getFile(
            @PathVariable final Long fileId,
            @RequestParam final boolean download
    ) {

        // 1) 파일 다운로드 로직 수행
        final FileDownloadResponseDto dto = fileService.getFile(fileId);

        // 2) 헤더 정보 제작
        final String encodedName = UriUtils.encode(dto.name(), StandardCharsets.UTF_8);
        final String disposition = (download ? "attachment; " : "inline; ")
                + "filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dto.mimeType()))
                .contentLength(dto.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(dto.resource());
    }
}
