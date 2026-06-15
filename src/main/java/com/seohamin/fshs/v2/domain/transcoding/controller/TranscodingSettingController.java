package com.seohamin.fshs.v2.domain.transcoding.controller;

import com.seohamin.fshs.v2.domain.transcoding.dto.TranscodingSettingResponseDto;
import com.seohamin.fshs.v2.domain.transcoding.dto.TranscodingSettingUpdateRequestDto;
import com.seohamin.fshs.v2.domain.transcoding.service.TranscodingSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/transcoding")
@RequiredArgsConstructor
public class TranscodingSettingController {

    private final TranscodingSettingService transcodingSettingService;

    // 현재 트랜스코딩 설정 조회 (어드민 전용)
    @GetMapping("/settings")
    public ResponseEntity<TranscodingSettingResponseDto> getSettings(
            @AuthenticationPrincipal final UserDetails userDetails
    ) {
        return ResponseEntity.ok(transcodingSettingService.getSettings(userDetails.getUsername()));
    }

    // 트랜스코딩 설정 변경 (어드민 전용)
    @PutMapping("/settings")
    public ResponseEntity<TranscodingSettingResponseDto> updateSettings(
            @AuthenticationPrincipal final UserDetails userDetails,
            @RequestBody final TranscodingSettingUpdateRequestDto request
    ) {
        return ResponseEntity.ok(transcodingSettingService.updateSettings(request, userDetails.getUsername()));
    }
}