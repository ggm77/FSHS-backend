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
        Category category,
        String name,
        String baseName,
        String extension
) {

    // FFprobe와 metadata-extractor 결과로부터 정보 추출
    public static FileAnalysisResultDto from(
            final FfmpegAnalysisResultDto ffmpegDto,
            final MetadataAnalysisResultDto metadataExtractorDto,
            final FilePropertiesDto metadataDto
    ) {
        var videoStream = ffmpegDto.streams().stream()
                .filter(FfmpegAnalysisResultDto.FfprobeStream::isVideo)
                .findFirst();

        var format = ffmpegDto.format();

        return new FileAnalysisResultDto(
                ffmpegDto.getVideoCodecs(),
                ffmpegDto.getAudioCodecs(),
                videoStream.map(s -> parseFps(s.avg_frame_rate())).orElse(0.0),
                format.format_name(),
                parseDuration(format.duration()),
                parseSafeInt(format.bit_rate()),
                metadataExtractorDto.capturedAt() != null
                        ? metadataExtractorDto.capturedAt()
                        : parseInstant(format.tags().creation_time()),
                videoStream.map(FfmpegAnalysisResultDto.FfprobeStream::width).orElse(0),
                videoStream.map(FfmpegAnalysisResultDto.FfprobeStream::height).orElse(0),
                metadataExtractorDto.orientation(),
                metadataExtractorDto.latLon() != null
                        ? parseLocation(metadataExtractorDto.latLon(), true)
                        : parseLocation(ffmpegDto.getLocation(), true),
                metadataExtractorDto.latLon() != null
                        ? parseLocation(metadataExtractorDto.latLon(), false)
                        : parseLocation(ffmpegDto.getLocation(), false),
                metadataDto.mimeType(),
                metadataDto.size(),
                metadataDto.category(),
                metadataDto.name(),
                metadataDto.baseName(),
                metadataDto.extension()
        );
    }

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
                parseLocation(dto.getLocation(), true),
                parseLocation(dto.getLocation(), false),
                metadataDto.mimeType(),
                metadataDto.size(),
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
                parseLocation(dto.latLon(), true),
                parseLocation(dto.latLon(), false),
                metadataDto.mimeType(),
                metadataDto.size(),
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
        if (location == null || location.isBlank()) return null;

        try {
            // 부호(+/-)로 시작하고 숫자와 소수점을 포함하는 패턴 추출
            var matcher = java.util.regex.Pattern.compile("[+-]\\d+\\.?\\d*").matcher(location);
            var coordinates = new java.util.ArrayList<Double>();

            while (matcher.find()) {
                coordinates.add(Double.parseDouble(matcher.group()));
            }

            // 결과 리스트: [위도, 경도, (선택적)고도]
            if (coordinates.size() < 2) return null;

            return isLat ? coordinates.get(0) : coordinates.get(1);
        } catch (Exception e) {
            return null;
        }
    }
}
