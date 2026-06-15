package com.seohamin.fshs.v2.domain.transcoding.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HwAccel {

    NONE(""),
    CUDA("cuda"),
    QSV("qsv"),
    VIDEOTOOLBOX("videotoolbox"),
    V4L2M2M("v4l2m2m"),
    DRM("drm");

    // ffmpeg의 -hwaccel 옵션에 들어가는 값 (NONE이면 옵션 미지정)
    private final String ffmpegValue;

    // YAML 등에서 온 문자열을 enum으로 변환 (빈 값/미일치는 NONE)
    public static HwAccel fromFfmpegValue(final String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        for (final HwAccel hwAccel : values()) {
            if (hwAccel.ffmpegValue.equals(value)) {
                return hwAccel;
            }
        }
        return NONE;
    }
}