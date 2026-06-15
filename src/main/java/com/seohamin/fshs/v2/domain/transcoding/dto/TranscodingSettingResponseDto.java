package com.seohamin.fshs.v2.domain.transcoding.dto;

import com.seohamin.fshs.v2.domain.transcoding.entity.H264Encoder;
import com.seohamin.fshs.v2.domain.transcoding.entity.HwAccel;
import com.seohamin.fshs.v2.domain.transcoding.entity.TranscodingQuality;
import com.seohamin.fshs.v2.domain.transcoding.entity.TranscodingSetting;

import java.util.List;

public record TranscodingSettingResponseDto(
        HwAccel hwAccel,
        H264Encoder h264Encoder,
        TranscodingQuality quality,
        List<HwAccel> availableHwAccels,
        List<H264Encoder> availableEncoders,
        List<TranscodingQuality> availableQualities
) {
    public static TranscodingSettingResponseDto of(final TranscodingSetting setting) {
        return new TranscodingSettingResponseDto(
                setting.getHwAccel(),
                setting.getH264Encoder(),
                setting.getQuality(),
                List.of(HwAccel.values()),
                List.of(H264Encoder.values()),
                List.of(TranscodingQuality.values())
        );
    }
}