package com.seohamin.fshs.v2.domain.file.controller;

import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileStatusResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileUploadResponseDto;
import com.seohamin.fshs.v2.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
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
    public ResponseEntity<FileUploadResponseDto> uploadFile(
            @RequestPart(value = "file") final MultipartFile multipartFile,
            @RequestParam(value = "lastModified") final Instant lastModified,
            @RequestParam(value = "folderId") final Long folderId
    ) {

        return ResponseEntity.ok(fileService.uploadFile(multipartFile, lastModified, folderId));
    }

    @GetMapping("/files/{fileUuid}/status")
    public ResponseEntity<FileStatusResponseDto> getFileStatus(
            @PathVariable final String fileUuid
    ) {

        return ResponseEntity.ok(fileService.getFileStatus(fileUuid));
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
    public ResponseEntity<?> getFile(
            @PathVariable final Long fileId,
            @RequestParam final boolean download,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) final String rangeHeader
    ) {

        // 1) 파일 다운로드 로직 수행
        final FileDownloadResponseDto dto = fileService.getFile(fileId);

        // 2) 공통 헤더 준비
        final String encodedName = UriUtils.encode(dto.name(), StandardCharsets.UTF_8);
        final String disposition = (download ? "attachment; " : "inline; ")
                + "filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName;
        final MediaType mediaType = MediaType.parseMediaType(dto.mimeType());

        // 3) Range 요청 처리 (부분 다운로드)
        if (rangeHeader != null) {
            final List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (!ranges.isEmpty()) {
                final long fileSize = dto.size();
                final HttpRange range = ranges.get(0);
                final long start = range.getRangeStart(fileSize);
                final long end = range.getRangeEnd(fileSize);

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(mediaType)
                        .contentLength(end - start + 1)
                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                        .body(new ResourceRegion(dto.resource(), start, end - start + 1));
            }
        }

        // 4) 전체 파일 응답
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(dto.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(dto.resource());
    }

    // 실시간 트랜스코딩을 통해 스트리밍하는 API
    @GetMapping("/files/{fileId}/stream")
    public ResponseEntity<StreamingResponseBody> streamFile(
            @PathVariable final Long fileId,
            @RequestParam(defaultValue = "0") final double start
    ) {

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(fileService.streamFile(fileId, start));
    }
}
