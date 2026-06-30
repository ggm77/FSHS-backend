package com.seohamin.fshs.v2.domain.auth.file.controller;

import com.seohamin.fshs.v2.domain.auth.file.service.SharedFileService;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class SharedFileController {

    private final SharedFileService sharedFileService;

    // 공유한 파일 정보 조회 API
    @GetMapping("/files/{shareKey}")
    public ResponseEntity<FileResponseDto> getSharedFileDetail(
            @PathVariable final String shareKey
    ) {

        return ResponseEntity.ok().body(sharedFileService.getSharedFileDetail(shareKey));
    }
}
