package com.seohamin.fshs.v2.global.infra.storage;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.ffmpeg.FfmpegProcessor;
import com.seohamin.fshs.v2.global.infra.ffmpeg.dto.FfmpegAnalysisResultDto;
import com.seohamin.fshs.v2.global.infra.metadataExtractor.MetadataAnalyzer;
import com.seohamin.fshs.v2.global.infra.metadataExtractor.dto.MetadataAnalysisResultDto;
import com.seohamin.fshs.v2.global.infra.storage.dto.FileAnalysisResultDto;
import com.seohamin.fshs.v2.global.infra.storage.dto.FilePropertiesDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 파일의 정보를 추출하고 검사하는 클래스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileAnalyzer {

    private final FfmpegProcessor ffmpegProcessor;
    private final MetadataAnalyzer metadataAnalyzer;
    private final StorageManager storageManager;

    /**
     * 파일 저장 전 정보 추출과 검사하는 메서드
     * FSHS에서 실행 할 수 있는 파일만 검증함.
     * @param tempFilePath 임시 파일의 경로
     * @return 파일 정보가 담긴 DTO
     */
    public FileAnalysisResultDto analyzeFile(final Path tempFilePath) {
        // 1) null 검사
        if (tempFilePath == null) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        // 2) 파일에서 기본 데이터 추출
        final FilePropertiesDto fileProperties = storageManager.getFileProperties(tempFilePath);

        // 3) 변수에 카테고리 저장
        final Category category = fileProperties.category();

        // 2) 카테고리에 따라 적절한 메서드 호출
        if(category.equals(Category.VIDEO)){
            // 영상 파일 정보 추출
            return FileAnalysisResultDto.from(
                    analyzeVideoFile(tempFilePath),
                    fileProperties
            );
        }
        else if (category.equals(Category.AUDIO)){
            // 오디오 파일 정보 추출
            return FileAnalysisResultDto.from(
                    analyzeAudioFile(tempFilePath),
                    fileProperties
            );
        }
        else if (category.equals(Category.IMAGE)){
            // 이미지 파일 정보 추출
            return FileAnalysisResultDto.from(
                    analyzeImageFile(tempFilePath),
                    fileProperties
            );
        }
        else {
            // 기타 파일 정보 추출
            return FileAnalysisResultDto.from(fileProperties);
        }
    }

    // 영상 파일 정보 추출
    private FfmpegAnalysisResultDto analyzeVideoFile(final Path tempFilePath) {
        // 1) FFprobe 호출해서 정보 추출
        final FfmpegAnalysisResultDto analysisResult = ffmpegProcessor.analyze(tempFilePath);

        // 2) 영상 파일 맞는지 검사
        if (
                analysisResult.streams()
                        .stream().noneMatch(FfmpegAnalysisResultDto.FfprobeStream::isVideo)
        ) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        return analysisResult;
    }

    // 오디오 파일 정보 추출
    private FfmpegAnalysisResultDto analyzeAudioFile(final Path tempFilePath) {
        // 1) FFprobe 호출해서 정보 추출
        final FfmpegAnalysisResultDto analysisResult = ffmpegProcessor.analyze(tempFilePath);

        // 2) 오디오 파일 맞는지 검사
        if (
                analysisResult.streams()
                        .stream().noneMatch(FfmpegAnalysisResultDto.FfprobeStream::isAudio)
        ) {
            throw new CustomException(ExceptionCode.INVALID_FILE);
        }

        return analysisResult;
    }

    // 이미지 파일 정보 추출
    private MetadataAnalysisResultDto analyzeImageFile(final Path tempFilePath) {

        return metadataAnalyzer.analyze(tempFilePath);
    }
}
