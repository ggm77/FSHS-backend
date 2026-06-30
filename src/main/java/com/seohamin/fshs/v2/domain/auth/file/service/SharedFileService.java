package com.seohamin.fshs.v2.domain.auth.file.service;

import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class SharedFileService {

    private final SharedFileRepository sharedFileRepository;
    private final StorageManager storageManager;

    /**
     * 공유 파일의 상세 정보 가져오는 메서드
     * @param shareKey 공유키
     * @return 파일의 상세 정보
     */
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

    /**
     * 공유 파일 다운로드하는 메서드
     * @param shareKey 다운할 파일의 공유키
     * @return 바이너리가 담긴 DTO
     */
    public FileDownloadResponseDto getSharedFile(final String shareKey) {
        // 1) null 검사
        if (shareKey == null || shareKey.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 키 조회
        final SharedFile sharedFile = sharedFileRepository.findByShareKey(shareKey)
                .orElseThrow(() -> new CustomException(ExceptionCode.SHARE_KEY_NOT_FOUND));

        // 3) 파일 엔티티 가져오기
        final File file =  sharedFile.getFile();

        // 4) 파일 읽어오기
        final Resource resource = storageManager.getFile(Path.of(file.getRelativePath()));

        // 5) DTO 조립
        return new FileDownloadResponseDto(
                file.getName(),
                file.getMimeType(),
                file.getSize(),
                resource
        );
    }
}
