package com.seohamin.fshs.v2.domain.file.controller;

import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<FileResponseDto> getFile(
            @PathVariable final Long fileId
    ) {

        return ResponseEntity.ok().body(fileService.getFileDetails(fileId));
    }
}
