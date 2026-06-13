package com.seohamin.fshs.v2.domain.file.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.entity.Status;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.FileAnalyzer;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import com.seohamin.fshs.v2.global.infra.storage.dto.FileAnalysisResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;

/**
 * 업로드 된 파일을 비동기로 분석/이동/DB 저장 처리하는 클래스
 * FileService에서 self-invocation 문제로 @Async가 작동하지 않는 것을 방지하기 위해
 * 별도 빈으로 분리됨
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadProcessor {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final StorageManager storageManager;
    private final FileAnalyzer fileAnalyzer;
    private final Cache<String, Status> fileStatusCache;

    /**
     * 임시 폴더에 저장된 파일을 분석/이동/DB 저장까지 비동기로 처리하는 메서드
     * @param fileUuid 상태 관리용 UUID
     * @param tempFilePath 요청 스레드에서 미리 임시 폴더에 저장된 파일 경로
     * @param parentFolderId 상위 폴더 ID (트랜잭션 내에서 재조회)
     * @param lastModified 파일의 마지막 수정 시점
     */
    @Async("asyncExecutor")
    @Transactional
    public void process(
            final String fileUuid,
            final Path tempFilePath,
            final Long parentFolderId,
            final Instant lastModified
    ) {
        try {
            // 1) 상위 폴더 재조회 - 트랜잭션 내에서 로드해야 영속성 컨텍스트에 붙음
            final Folder parentFolder = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));
            final Path parentFolderPath = Path.of(parentFolder.getRelativePath());

            // 2) 카테고리에 맞춰서 파일 검증 및 정보 추출
            final FileAnalysisResultDto analysisResult = fileAnalyzer.analyzeFile(tempFilePath);
            log.info("[파일 검증 및 정보 추출 완료]: {}", fileUuid);

            // 3) 파일 원본 위치로 이동
            final Path savedPath = storageManager.savePermanently(tempFilePath, parentFolderPath, lastModified);
            log.info("[파일 저장 완료]: {}", fileUuid);

            // 4) 파일 엔티티 생성
            final File file = File.builder()
                    .parentFolder(parentFolder)
                    .uuid(fileUuid)
                    .name(analysisResult.name())
                    .baseName(analysisResult.baseName())
                    .extension(analysisResult.extension())
                    .relativePath(savedPath.toString())
                    .parentPath(parentFolderPath.toString())
                    .mimeType(analysisResult.mimeType())
                    .size(analysisResult.size())
                    .videoCodec(analysisResult.videoCodec())
                    .audioCodec(analysisResult.audioCodec())
                    .width(analysisResult.width())
                    .height(analysisResult.height())
                    .duration(analysisResult.duration())
                    .bitrate(analysisResult.bitrate())
                    .orientation(analysisResult.orientation())
                    .lat(analysisResult.lat())
                    .lon(analysisResult.lon())
                    .fps(analysisResult.fps())
                    .format(analysisResult.format())
                    .capturedAt(analysisResult.capturedAt())
                    .originUpdatedAt(lastModified)
                    .category(analysisResult.category())
                    .build();

            // 5) DB에 저장
            fileRepository.save(file);
            log.info("[파일 정보 DB에 저장 완료]: {}", fileUuid);

            // 6) 캐시 상태 완료로 변경
            fileStatusCache.put(fileUuid, Status.COMPLETE);
        } catch (final CustomException ex) {
            storageManager.deleteTemporaryFile(tempFilePath);
            fileStatusCache.put(fileUuid, Status.ERROR);
            log.error("[파일 처리 중 에러 발생]: {}, {}, {}",
                    fileUuid, ex.getExceptionCode(), ex.getExceptionCode().getMessage());
        } catch (final Exception ex) {
            storageManager.deleteTemporaryFile(tempFilePath);
            fileStatusCache.put(fileUuid, Status.ERROR);
            log.error("[파일 처리 중 에러 발생]: {}, {}", fileUuid, ex.getMessage(), ex);
        }
    }
}
