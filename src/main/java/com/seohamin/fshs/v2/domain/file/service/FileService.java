package com.seohamin.fshs.v2.domain.file.service;

import com.seohamin.fshs.v2.domain.file.dto.FileResponseDto;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.FileAnalyzer;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import com.seohamin.fshs.v2.global.infra.storage.dto.FileAnalysisResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final StorageManager storageManager;
    private final FileAnalyzer fileAnalyzer;

    /**
     * 파일 하나 업로드처리하는 메서드
     *
     * @param multipartFile 업로드할 파일
     * @param lastModified 업로드할 파일의 마지막 수정 시점
     * @return 업로드된 파일의 정보
     */
    @Transactional
    public FileResponseDto uploadFile(
            final MultipartFile multipartFile,
            final Instant lastModified,
            final Long folderId
    ) {

        // 1) null 검사
        if (multipartFile == null || lastModified == null || folderId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 시스템 루트 검사
        if (folderId == 1L) {
            throw new CustomException(ExceptionCode.SYSTEM_ROOT_FORBIDDEN);
        }

        // 3) 상위 폴더 조회
        final Folder parentFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 4) 업로드 메서드 호출
        return processUpload(multipartFile, lastModified, parentFolder);
    }

    /**
     * 파일의 정보를 조회하는 메서드
     * @param fileId 조회할 파일 ID
     * @return 파일 정보 담긴 DTO
     */
    public FileResponseDto getFileDetails(final Long fileId) {
        // 1) null 검사
        if(fileId == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 정보 조회
        final File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FILE_NOT_EXIST));

        // 3) DTO에 담기
        return FileResponseDto.of(file);
    }

    /**
     * 실제로 파일을 검증하고 저장하는 메서드
     * DB에 정보를 저장하는 부분 외에는 전부 유틸 메서드를 통해 진행
     * @param multipartFile 저장할 파일
     * @param lastModified 저장할 파일의 마지막 수정 시점 정보
     * @param parentFolder 상위 폴더
     * @return 저장된 파일의 정보가 담긴 DTO
     */
    private FileResponseDto processUpload(
            final MultipartFile multipartFile,
            final Instant lastModified,
            final Folder parentFolder
    ) {
        // 1) 변수에 값 저장
        final Path parentFolderPath = Path.of(parentFolder.getRelativePath());

        // 2) 파일 임시 폴더에 저장
        final Path tempFilePath = storageManager.saveTemporarily(multipartFile);

        // 3) 카테고리에 맞춰서 파일 검증 및 정보 추출
        final FileAnalysisResultDto analysisResult = fileAnalyzer.analyzeFile(tempFilePath);

        // 4) 파일 원본 위치로 이동
        final Path savedPath = storageManager.savePermanently(tempFilePath, parentFolderPath);

        // 5) 파일 엔티티 생성 - 파일명, 확장자는 소문자로만 저장, 경로는 그대로
        final File file = File.builder()
                .parentFolder(parentFolder)
                .name(analysisResult.name().toLowerCase())
                .baseName(analysisResult.baseName().toLowerCase())
                .extension(analysisResult.extension().toLowerCase())
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

        // 6) DB에 저장
        final File savedFile = fileRepository.save(file);

        return FileResponseDto.of(savedFile);
    }
}
