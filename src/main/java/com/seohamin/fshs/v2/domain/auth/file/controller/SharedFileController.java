package com.seohamin.fshs.v2.domain.auth.file.controller;

import com.seohamin.fshs.v2.domain.auth.file.dto.SharedFileResponseDto;
import com.seohamin.fshs.v2.domain.auth.file.service.SharedFileService;
import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth")
public class SharedFileController {

    private final SharedFileService sharedFileService;

    // 공유한 파일 정보 조회 API
    @GetMapping("/files/{shareKey}")
    public ResponseEntity<SharedFileResponseDto> getSharedFileDetail(
            @PathVariable final String shareKey
    ) {

        return ResponseEntity.ok().body(sharedFileService.getSharedFileDetail(shareKey));
    }

    // 파일 다운로드 & 직접 스트리밍 API
    @GetMapping("/files/{shareKey}/content")
    public ResponseEntity<ResourceRegion> getSharedFile(
            @PathVariable final String shareKey,
            @RequestParam final boolean download,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) final String rangeHeader
    ) {

        // 1) 다운로드 로직
        final FileDownloadResponseDto dto = sharedFileService.getSharedFile(shareKey);

        // 2) 공통 헤더 준비
        final String encodedName = UriUtils.encode(dto.name(), StandardCharsets.UTF_8);
        final String disposition = (download ? "attachment; " : "inline; ")
                + "filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName;
        final MediaType mediaType = MediaType.parseMediaType(dto.mimeType());
        final long fileSize = dto.size();

        // 3) Range 파싱 — 없거나 형식이 잘못되면 빈 목록으로 보고 전체 파일을 200으로 반환 (RFC 7233)
        List<HttpRange> ranges;
        try {
            ranges = HttpRange.parseRanges(rangeHeader);
        } catch (final IllegalArgumentException ex) {
            ranges = List.of();
        }

        // 4) Range 없으면 전체 파일을 200으로 반환
        //    (ResourceRegion 컨버터를 쓰기 위해 전체 구간을 ResourceRegion 으로 감싼다)
        if (ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(new ResourceRegion(dto.resource(), 0, fileSize));
        }

        // 5) 첫 구간 계산 — 파일 크기를 벗어나면 416 반환
        final HttpRange range = ranges.getFirst();
        final long start = range.getRangeStart(fileSize);
        final long end = range.getRangeEnd(fileSize);
        if (start >= fileSize) {
            return ResponseEntity
                    .status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        // 6) 부분 응답 206 (Content-Range 는 ResourceRegion 직렬화 시 자동 설정됨)
        final long contentLength = end - start + 1;
        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new ResourceRegion(dto.resource(), start, contentLength));
    }

    // 실시간 h264 트랜스코딩 API
    @GetMapping("/files/{shareKey}/stream")
    public ResponseEntity<StreamingResponseBody> streamSharedFile(
            @PathVariable final String shareKey,
            @RequestParam(defaultValue = "0") final double start
    ) {

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "none")
                .body(sharedFileService.streamSharedFile(shareKey, start));
    }

    // 실시간 hls 트랜스코딩 API
    @GetMapping("/files/{shareKey}/stream/{hlsFile}")
    public ResponseEntity<InputStreamResource> streamSharedHlsFile(
            @PathVariable final String shareKey,
            @PathVariable final String hlsFile
    ) {

        // 재생목록(.m3u8)과 세그먼트(.ts)는 Content-Type이 다름
        final MediaType mediaType = hlsFile.endsWith(".m3u8")
                ? MediaType.parseMediaType("application/x-mpegURL")
                : MediaType.parseMediaType("video/mp2t");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(sharedFileService.streamSharedHlsFile(shareKey, hlsFile));
    }

    // 공유파일 썸네일 조회 API
    @GetMapping("/files/{shareKey}/thumbnail")
    public ResponseEntity<Resource> getSharedFileThumbnail(
            @PathVariable final String shareKey
    ) {
        final FileDownloadResponseDto dto = sharedFileService.getSharedFileThumbnail(shareKey);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dto.mimeType()))
                .contentLength(dto.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + dto.name() + "\"")
                .body(dto.resource());
    }
}
