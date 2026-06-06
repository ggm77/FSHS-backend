package com.seohamin.fshs.v2.domain.file.controller;

import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileStatusResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileUploadResponseDto;
import com.seohamin.fshs.v2.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // 파일 단건 업로드 API
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponseDto> uploadFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "file") final MultipartFile multipartFile,
            @RequestParam(value = "lastModified") final Instant lastModified,
            @RequestParam(value = "folderId") final Long folderId
    ) {

        return ResponseEntity.ok(fileService.uploadFile(multipartFile, lastModified, folderId, userDetails.getUsername()));
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
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable final Long fileId
    ) {

        return ResponseEntity.ok().body(fileService.getFileDetails(fileId, userDetails.getUsername()));
    }

    // 파일 다운로드 및 직접 스트리밍 API
    @GetMapping("/files/{fileId}/content")
    public ResponseEntity<ResourceRegion> getFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable final Long fileId,
            @RequestParam final boolean download,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) final String rangeHeader
    ) {

        // 1) 파일 다운로드 로직 수행
        final FileDownloadResponseDto dto = fileService.getFile(fileId, userDetails.getUsername());

        // 2) 공통 헤더 준비
        final String encodedName = UriUtils.encode(dto.name(), StandardCharsets.UTF_8);
        final String disposition = (download ? "attachment; " : "inline; ")
                + "filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName;
        final MediaType mediaType = MediaType.parseMediaType(dto.mimeType());
        final long fileSize = dto.size();

        // 3) Range 파싱 (없으면 전체 파일)
        final long start;
        final long end;
        if (rangeHeader != null && !HttpRange.parseRanges(rangeHeader).isEmpty()) {
            final HttpRange range = HttpRange.parseRanges(rangeHeader).getFirst();
            start = range.getRangeStart(fileSize);
            end = range.getRangeEnd(fileSize);
        } else {
            start = 0;
            end = fileSize - 1;
        }

        final boolean isPartial = rangeHeader != null;
        final long contentLength = end - start + 1;

        final ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(isPartial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .contentType(mediaType)
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes");

        if (isPartial) {
            builder.header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
        }

        return builder.body(new ResourceRegion(dto.resource(), start, contentLength));
    }

    // 실시간 트랜스코딩을 통해 스트리밍하는 API
    @GetMapping("/files/{fileId}/stream")
    public ResponseEntity<StreamingResponseBody> streamFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable final Long fileId,
            @RequestParam(defaultValue = "0") final double start
    ) {

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(fileService.streamFile(fileId, start, userDetails.getUsername()));
    }

    // 파일 휴지통으로 보내는 API
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable final Long fileId
    ) {

        fileService.deleteFile(fileId, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }
}
