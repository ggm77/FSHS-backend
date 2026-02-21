package com.seohamin.fshs.v2.global.infra.metadataExtractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.metadataExtractor.dto.MetadataAnalysisResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

@Component
@Slf4j
public class MetadataAnalyzer {

    /**
     * 파일의 메타데이터를 추출하는 메서드
     * 이미지 파일에서 추출하는걸 기본으로 만들어짐
     * @param filePath 메타데이터 추출할 파일의 Path
     * @return 추출된 데이터의 DTO
     */
    public MetadataAnalysisResultDto analyze(final Path filePath) {
        // 파일 IO 성능 높이기 위해 사용
        try (InputStream is = new BufferedInputStream(Files.newInputStream(filePath))){

            // 파일 스트림을 Metadata로 파싱
            final Metadata metadata = ImageMetadataReader.readMetadata(is);

            // 촬영시각, 해상도 등 카메라 설정 정보
            final ExifSubIFDDirectory subIFD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            // 위도, 경도, 고도 등 위치 정보
            final GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);

            // 이미지 전반 요약 정보
            final ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            // 애플 GPS 정보
            final QuickTimeMetadataDirectory qmd = metadata.getFirstDirectoryOfType(QuickTimeMetadataDirectory.class);

            // 결과 담는 DTO에 넣기
            return new MetadataAnalysisResultDto(
                    extractShotDate(subIFD),
                    extractOrientation(ifd0),
                    extractGps(gps, qmd),
                    extractWidth(subIFD, ifd0),
                    extractHeight(subIFD, ifd0)
            );

        } catch (final Exception ex) {
            log.error("[이미지 파일 분석중 에러] {}", ex.getMessage());
            throw new CustomException(ExceptionCode.METADATA_EXTRACTOR_ERROR, ex);
        }
    }

    // 사진 찍은 날짜 추출
    private Instant extractShotDate(final ExifSubIFDDirectory subIFD) {
        if (subIFD == null) {
            return null;
        }

        final Date date = subIFD.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        if (date != null) {
            return date.toInstant();
        } else {
            return null;
        }
    }

    // lat lon 추출
    private String extractGps(
            final GpsDirectory gps,
            final QuickTimeMetadataDirectory qmd
    ) {
        if (gps != null && gps.getGeoLocation() != null) {
            var loc = gps.getGeoLocation();
            return String.format("%+f%+f/", loc.getLatitude(), loc.getLongitude());
        }

        if (qmd != null) {
            final String location = qmd.getString(QuickTimeMetadataDirectory.TAG_LOCATION_ISO6709);
            if (location != null) {
                return location;
            }
        }

        return null;
    }

    // 이미지 너비 추출
    private Integer extractWidth(
            final ExifSubIFDDirectory subIFD,
            final ExifIFD0Directory ifd0
    ) {
        if (subIFD != null && subIFD.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)) {
            return subIFD.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
        }
        if (ifd0 != null) {
            return ifd0.getInteger(ExifIFD0Directory.TAG_IMAGE_WIDTH);
        } else {
            return null;
        }
    }

    // 이미지 높이 추출
    private Integer extractHeight(
            final ExifSubIFDDirectory subIFD,
            final ExifIFD0Directory ifd0
    ) {
        if (subIFD != null && subIFD.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)) {
            return subIFD.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
        }

        if (ifd0 != null) {
            return ifd0.getInteger(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
        } else {
            return null;
        }
    }

    // 이미지 회전 정보 추출
    private Integer extractOrientation(final ExifIFD0Directory ifd0) {

        if (ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
            return ifd0.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
        } else {
            return 1;
        }
    }
}
