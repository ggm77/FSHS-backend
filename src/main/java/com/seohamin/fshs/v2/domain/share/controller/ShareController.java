package com.seohamin.fshs.v2.domain.share.controller;

import com.seohamin.fshs.v2.domain.share.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class ShareController {

    private final ShareService shareService;

    // 생성된 공유키 삭제하는 API
    @DeleteMapping("/shares/{shareId}")
    public ResponseEntity<Void> deleteShareKey(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long shareId
    ) {

        shareService.deleteShareKey(shareId, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

}
