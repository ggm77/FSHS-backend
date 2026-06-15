package com.seohamin.fshs.v2.domain.transcoding.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum H264Encoder {

    LIBX264("libx264"),
    H264_NVENC("h264_nvenc"),
    H264_QSV("h264_qsv"),
    H264_VIDEOTOOLBOX("h264_videotoolbox"),
    H264_V4L2M2M("h264_v4l2m2m");

    // ffmpeg의 -vcodec 옵션에 들어가는 값
    private final String ffmpegValue;

    // YAML 등에서 온 문자열을 enum으로 변환 (빈 값/미일치는 libx264)
    public static H264Encoder fromFfmpegValue(final String value) {
        if (value == null || value.isBlank()) {
            return LIBX264;
        }
        for (final H264Encoder encoder : values()) {
            if (encoder.ffmpegValue.equals(value)) {
                return encoder;
            }
        }
        return LIBX264;
    }
}