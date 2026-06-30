package com.seohamin.fshs.v2.domain.auth.file.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.service.FileThumbnailProcessor;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.ffmpeg.FfmpegProcessor;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SharedFileService {

    private final SharedFileRepository sharedFileRepository;
    private final StorageManager storageManager;
    private final Cache<String, String> sharedFilePathCache;
    private final FfmpegProcessor ffmpegProcessor;
    private final FileThumbnailProcessor fileThumbnailProcessor;

    // HLS 세그먼트 파일명 패턴 (예: segment12.ts) — 자릿수를 제한해 int 오버플로 방지
    private static final Pattern HLS_SEGMENT_PATTERN = Pattern.compile("segment(\\d{1,9})\\.ts");

    /**
     * 공유 파일의 상세 정보 가져오는 메서드
     * @param shareKey 공유키
     * @return 파일의 상세 정보
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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

    /**
     * 실시간 h264 트랜스코딩 스트리밍하는 메서드
     * @param shareKey 공유키
     * @param start 영상 시작 지점
     * @return 트랜스코딩된 스트리밍 바디
     */
    @Transactional(readOnly = true)
    public StreamingResponseBody streamSharedFile(
            final String shareKey,
            final double start
    ) {
        // 1) null 검사
        if (shareKey == null || shareKey.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 캐시에서 공유키 이용해서 파일 위치 확인
        final String path = sharedFilePathCache.get(
                shareKey, key -> sharedFileRepository.findByShareKey(shareKey)
                        .map(SharedFile::getFile)
                        .map(File::getRelativePath)
                        .orElseThrow(() -> new CustomException(ExceptionCode.SHARE_KEY_NOT_FOUND))
        );

        // 3) 절대 경로로 변환
        final Path absPath = storageManager.resolvePath(path, false);

        // 4) 실시간 트랜스코딩
        return ffmpegProcessor.getVideoStream(absPath.toString(), start);
    }

    /**
     * 공유파일 HLS 실시간 트랜스코딩하는 메서드
     * @param shareKey 공유키
     * @param hlsFile 요청한 HLS 파일
     * @return HLS 스트림
     */
    @Transactional(readOnly = true)
    public InputStreamResource streamSharedHlsFile(
            final String shareKey,
            final String hlsFile
    ) {
        // 1) null 검사
        if (shareKey == null || shareKey.isBlank() ||  hlsFile == null || hlsFile.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 캐시에서 공유키 이용해서 파일 위치 확인
        final String path = sharedFilePathCache.get(
                shareKey, key -> sharedFileRepository.findByShareKey(shareKey)
                        .map(SharedFile::getFile)
                        .map(File::getRelativePath)
                        .orElseThrow(() -> new CustomException(ExceptionCode.SHARE_KEY_NOT_FOUND))
        );

        // 3) 절대 경로로 변환
        final Path absPath = storageManager.resolvePath(path, false);

        // 4) 재생목록(.m3u8) 요청이면 DB duration으로 즉시 생성해 반환 (ffprobe 불필요)
        if (hlsFile.endsWith(".m3u8")) {
            final SharedFile sharedFile = sharedFileRepository.findByShareKey(shareKey)
                    .orElseThrow(() -> new CustomException(ExceptionCode.SHARE_KEY_NOT_FOUND));
            return ffmpegProcessor.getHlsPlaylist(sharedFile.getFile().getDuration());
        }

        // 5) 세그먼트(.ts) 요청이면 인덱스를 파싱해 해당 구간만 실시간 트랜스코딩
        final Matcher matcher = HLS_SEGMENT_PATTERN.matcher(hlsFile);
        if (!matcher.matches()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        return ffmpegProcessor.getHlsSegment(absPath, Integer.parseInt(matcher.group(1)));
    }

    /**
     * 공유 파일 썸네일 가져오는 메서드
     * @param shareKey 공유키
     * @return 썸네일 파일
     */
    @Transactional(readOnly = true)
    public FileDownloadResponseDto getSharedFileThumbnail(
            final String shareKey
    ) {
        // 1) null 검사
        if (shareKey == null || shareKey.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 키 조회
        final SharedFile sharedFile = sharedFileRepository.findByShareKey(shareKey)
                .orElseThrow(() -> new CustomException(ExceptionCode.SHARE_KEY_NOT_FOUND));

        // 3) 파일의 UUID 조회
        final String uuid = sharedFile.getFile().getUuid();

        // 4) 썸네일 파일 조회
        final Path thumbnailPath = fileThumbnailProcessor.resolveThumbnailPath(uuid);
        if (Files.notExists(thumbnailPath) || !Files.isRegularFile(thumbnailPath)) {
            throw new CustomException(ExceptionCode.FILE_NOT_EXIST);
        }

        try {
            return new FileDownloadResponseDto(
                    uuid + ".jpg",
                    "image/jpeg",
                    Files.size(thumbnailPath),
                    new FileSystemResource(thumbnailPath)
            );
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FILE_READ_ERROR, ex);
        }
    }
}
