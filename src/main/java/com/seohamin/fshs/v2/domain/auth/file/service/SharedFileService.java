package com.seohamin.fshs.v2.domain.auth.file.service;

import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedFileService {

    private final SharedFileRepository sharedFileRepository;

    public FileResponseDto getSharedFileDetail(final String shareKey) {
        // 1) null 검사
        if (shareKey == null || shareKey.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 키 조회
        final SharedFile sharedFile = sharedFileRepository.findByShareKey(shareKey)
                .orElseThrow(() -> new CustomException(ExceptionCode.SHARE_KEY_NOT_FOUND));

        // 3) 파일 정보 리턴
        return FileResponseDto.of(sharedFile.getFile());
    }
}
