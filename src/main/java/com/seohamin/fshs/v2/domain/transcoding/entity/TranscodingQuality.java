package com.seohamin.fshs.v2.domain.transcoding.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TranscodingQuality {

    P480(854, 480),
    P720(1280, 720),
    P1080(1920, 1080);

    // 출력 해상도 상한 (scale 필터의 가로/세로 경계, 원본 비율 유지하며 축소)
    private final int width;
    private final int height;
}