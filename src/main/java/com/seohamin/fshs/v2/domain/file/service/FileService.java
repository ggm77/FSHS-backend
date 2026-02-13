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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${file.upload.max-count}")
    private int MAX_FILE_UPLOAD_COUNT;

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final StorageManager storageManager;
    private final FileAnalyzer fileAnalyzer;

    /**
     * 여러 파일들을 업로드처리하는 메서드
     *
     * @param multipartFiles 업로드할 파일 리스트
     * @return 업로드된 파일들의 정보 리스트
     */
    @Transactional
    public List<FileResponseDto> uploadFile(
            final List<MultipartFile> multipartFiles,
            final Long folderId
    ) {

        // 1) 업로드 된 파일 개수 확인
        final int fileCount = multipartFiles.size();
        if (fileCount > MAX_FILE_UPLOAD_COUNT) {
            throw new CustomException(ExceptionCode.TOO_MANY_FILES);
        }

        // 2) 상위 폴더 조회
        final Folder parentFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 3) 최종 리턴할 리스트 생성
        final List<FileResponseDto> responses = new ArrayList<>();

        // 4) 파일 개수만큼 반복문 실행해서 파일 저장 후 정보 가져옴
        for (final MultipartFile multipartFile : multipartFiles) {

            // 4-1) 파일 처리 및 리스트에 추가
            final FileResponseDto savedFile = processUpload(multipartFile, parentFolder);
            if(savedFile != null) {
                responses.add(savedFile);
            }
        }

        // 5) 최종 응답
        return responses;
    }

    /**
     * 실제로 파일을 검증하고 저장하는 메서드
     * DB에 정보를 저장하는 부분 외에는 전부 유틸 메서드를 통해 진행
     * @param multipartFile 저장할 단일 파일
     * @param parentFolder 상위 폴더
     * @return 저장된 파일의 정보가 담긴 DTO
     */
    private FileResponseDto processUpload(
            final MultipartFile multipartFile,
            final Folder parentFolder
    ) {
        // 1) 변수에 값 저장
        final Path parentFolderPath = Path.of(parentFolder.getRelativePath());

        // 2) 파일 임시 폴더에 저장
        final Path tempFilePath = storageManager.saveTemporarily(multipartFile);

        // 3) 카테고리에 맞춰서 파일 검증 및 정보 추출
        final FileAnalysisResultDto analysisResult = fileAnalyzer.analyzeFile(tempFilePath);

        // 4) 파일 원본 위치로 이동
        final Path savedPath = storageManager.moveFile(tempFilePath, parentFolderPath);

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
                .originCreatedAt(analysisResult.originCreatedAt())
                .originUpdatedAt(analysisResult.originUpdatedAt())
                .category(analysisResult.category())
                .build();

        // 6) DB에 저장
        final File savedFile = fileRepository.save(file);

        return FileResponseDto.of(savedFile);
    }
}
