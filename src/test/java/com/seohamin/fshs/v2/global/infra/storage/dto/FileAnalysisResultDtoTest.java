package com.seohamin.fshs.v2.global.infra.storage.dto;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.infra.ffmpeg.dto.FfmpegAnalysisResultDto;
import com.seohamin.fshs.v2.global.infra.metadataExtractor.dto.MetadataAnalysisResultDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FileAnalysisResultDtoTest {

    // ffprobe가 format 레벨 태그를 내려주지 않는 환경(일부 ffmpeg 빌드)을 재현
    private FfmpegAnalysisResultDto ffmpegResultWithoutFormatTags() {
        var format = new FfmpegAnalysisResultDto.FfprobeFormat("mp3", "10.0", "128000", null);
        return new FfmpegAnalysisResultDto(List.of(), format);
    }

    private FilePropertiesDto fileProperties() {
        return new FilePropertiesDto("audio/mpeg", 1234L, Category.AUDIO, "a.mp3", "a", "mp3");
    }

    @Test
    @DisplayName("format.tags가 null인 오디오 파일도 NPE 없이 분석되고 capturedAt은 null이다")
    void from_audio_nullFormatTags() {
        var result = FileAnalysisResultDto.from(ffmpegResultWithoutFormatTags(), fileProperties());

        assertThat(result.capturedAt()).isNull();
    }

    @Test
    @DisplayName("format.tags와 metadata capturedAt이 모두 null인 영상 파일도 NPE 없이 분석된다")
    void from_video_nullFormatTags() {
        var metadataExtractorDto = new MetadataAnalysisResultDto(null, null, null, null, null);

        assertThatCode(() -> FileAnalysisResultDto.from(
                ffmpegResultWithoutFormatTags(), metadataExtractorDto, fileProperties()
        )).doesNotThrowAnyException();
    }
}