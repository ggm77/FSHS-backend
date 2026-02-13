package com.seohamin.fshs.v2.global.infra.storage.dto;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.global.infra.ffmpeg.dto.FfmpegAnalysisResultDto;
import com.seohamin.fshs.v2.global.infra.metadataExtractor.dto.MetadataAnalysisResultDto;

import java.time.Instant;
import java.util.Optional;

public record FileAnalysisResultDto(
        String videoCodec,
        String audioCodec,
        Double fps,
        String format,
        Long duration,
        Integer bitrate,
        Instant capturedAt,
        Integer width,
        Integer height,
        Integer orientation,
        Double lat,
        Double lon,
        String mimeType,
        Long size,
        Instant originCreatedAt,
        Instant originUpdatedAt,
        Category category,
        String name,
        String baseName,
        String extension
) {

    // FFprobe 결과로부터 정보 추출
    public static FileAnalysisResultDto from(
            final FfmpegAnalysisResultDto dto,
            final FilePropertiesDto metadataDto
    ) {
        var videoStream = dto.streams().stream()
                .filter(FfmpegAnalysisResultDto.FfprobeStream::isVideo)
                .findFirst();

        var format = dto.format();

        return new FileAnalysisResultDto(
                dto.getVideoCodecs(),
                dto.getAudioCodecs(),
                videoStream.map(s -> parseFps(s.avg_frame_rate())).orElse(0.0),
                format.format_name(),
                parseDuration(format.duration()),
                parseSafeInt(format.bit_rate()),
                parseInstant(format.tags().creation_time()),
                videoStream.map(FfmpegAnalysisResultDto.FfprobeStream::width).orElse(0),
                videoStream.map(FfmpegAnalysisResultDto.FfprobeStream::height).orElse(0),
                videoStream.map(s -> parseSafeInt(s.tags() != null ? s.tags().rotate() : "0")).orElse(0),
                parseLocation(format.tags().location(), true),
                parseLocation(format.tags().location(), false),
                metadataDto.mimeType(),
                metadataDto.size(),
                metadataDto.originCreatedAt(),
                metadataDto.originUpdatedAt(),
                metadataDto.category(),
                metadataDto.name(),
                metadataDto.baseName(),
                metadataDto.extension()
        );
    }

    // metadata-extractor 결과로 부터 정보 추출
    public static FileAnalysisResultDto from(
            final MetadataAnalysisResultDto dto,
            final FilePropertiesDto metadataDto
    ) {
        return new FileAnalysisResultDto(
                "none",
                "none",
                null,
                "none",
                null,
                null,
                dto.capturedAt(),
                dto.width(),
                dto.height(),
                dto.orientation(),
                dto.lat(),
                dto.lon(),
                metadataDto.mimeType(),
                metadataDto.size(),
                metadataDto.originCreatedAt(),
                metadataDto.originUpdatedAt(),
                metadataDto.category(),
                metadataDto.name(),
                metadataDto.baseName(),
                metadataDto.extension()
        );
    }

    // 기타 파일
    public static FileAnalysisResultDto from(
            final FilePropertiesDto metadataDto
    ) {
        return new FileAnalysisResultDto(
                "none",
                "none",
                null,
                "none",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                metadataDto.mimeType(),
                metadataDto.size(),
                metadataDto.originCreatedAt(),
                metadataDto.originUpdatedAt(),
                metadataDto.category(),
                metadataDto.name(),
                metadataDto.baseName(),
                metadataDto.extension()
        );
    }

    private static Double parseFps(String avgFrameRate) {
        if (avgFrameRate == null || !avgFrameRate.contains("/")) return 0.0;
        try {
            String[] parts = avgFrameRate.split("/");
            double num = Double.parseDouble(parts[0]);
            double den = Double.parseDouble(parts[1]);

            // 0으로 나누는 예외 상황 방지
            return (den == 0) ? 0.0 : num / den;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static Long parseDuration(String duration) {
        try {
            return Optional.ofNullable(duration)
                    .map(d -> (long) Double.parseDouble(d))
                    .orElse(0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static Instant parseInstant(String creationTime) {
        try {
            return creationTime != null ? Instant.parse(creationTime) : null;
        } catch (Exception e) {
            return null; // 포맷이 다를 경우(예: 로컬 타임)에 대한 추가 처리 가능
        }
    }

    private static Integer parseSafeInt(String value) {
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static Double parseLocation(String location, boolean isLat) {
        if (location == null || location.isEmpty()) return null;
        try {
            // 정규식 등을 사용하여 좌표 추출 (+35.1234+129.5678/ 구조)
            String cleaned = location.replace("/", "");
            int secondSignIndex = cleaned.lastIndexOf('+') > 0 ? cleaned.lastIndexOf('+') : cleaned.lastIndexOf('-');

            if (isLat) return Double.parseDouble(cleaned.substring(0, secondSignIndex));
            else return Double.parseDouble(cleaned.substring(secondSignIndex));
        } catch (Exception e) {
            return null;
        }
    }
}
