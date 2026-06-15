package com.seohamin.fshs.v2.domain.transcoding.dto;

import com.seohamin.fshs.v2.domain.transcoding.entity.H264Encoder;
import com.seohamin.fshs.v2.domain.transcoding.entity.HwAccel;
import com.seohamin.fshs.v2.domain.transcoding.entity.TranscodingQuality;

public record TranscodingSettingUpdateRequestDto(
        HwAccel hwAccel,
        H264Encoder h264Encoder,
        TranscodingQuality quality
) { }