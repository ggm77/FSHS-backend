package com.seohamin.fshs.v2.domain.folder.controller;

import com.seohamin.fshs.v2.domain.folder.dto.FolderRequestDto;
import com.seohamin.fshs.v2.domain.folder.dto.FolderResponseDto;
import com.seohamin.fshs.v2.domain.folder.dto.SimpleFolderResponseDto;
import com.seohamin.fshs.v2.domain.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
}
