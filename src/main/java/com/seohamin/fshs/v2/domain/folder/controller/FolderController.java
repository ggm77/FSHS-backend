package com.seohamin.fshs.v2.domain.folder.controller;

import com.seohamin.fshs.v2.domain.folder.dto.FolderDownloadResponseDto;
import com.seohamin.fshs.v2.domain.folder.dto.FolderRequestDto;
import com.seohamin.fshs.v2.domain.folder.dto.FolderResponseDto;
import com.seohamin.fshs.v2.domain.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class FolderController {

    private final FolderService folderService;

    // 폴더 생성 API
    @PostMapping("/folders")
    public ResponseEntity<FolderResponseDto> createFolder(
            @AuthenticationPrincipal final UserDetails userDetails,
            @RequestBody final FolderRequestDto folderRequestDto
    ) {

        return ResponseEntity.ok().body(folderService.createFolder(folderRequestDto, userDetails.getUsername()));
    }

    // 폴더 정보 조회 API
    @GetMapping("/folders/{folderId}")
    public ResponseEntity<FolderResponseDto> getFolder(
            @PathVariable final Long folderId
    ) {

        return ResponseEntity.ok().body(folderService.getFolder(folderId));
    }

    // 폴더 다운로드 API
    @GetMapping("/folders/{folderId}/content")
    public ResponseEntity<StreamingResponseBody> downloadFolder(
            @PathVariable final Long folderId
    ) {
        final FolderDownloadResponseDto dto = folderService.downloadFolder(folderId);
        final String encodedName = UriUtils.encode(dto.name() + ".zip", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                .body(dto.stream());
    }
}
