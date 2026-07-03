package com.seohamin.fshs.v2.domain.system.controller;

import com.seohamin.fshs.v2.domain.system.dto.SystemStatusResponseDto;
import com.seohamin.fshs.v2.domain.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class SystemController {

    private final SystemService systemService;

    // 서버 상태 가져오는 API
    @GetMapping("/system/status")
    public ResponseEntity<SystemStatusResponseDto> getSystemStatus() {

        return ResponseEntity.ok(systemService.getSystemStatus());
    }
}
