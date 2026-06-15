package com.seohamin.fshs.v2.domain.transcoding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실시간 트랜스코딩 전역 설정 (서버당 단일 행, id=1 고정)
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "transcoding_setting")
public class TranscodingSetting {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @Column(columnDefinition = "integer")
    private Long id;

    // 하드웨어 가속기
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @NotNull
    private HwAccel hwAccel;

    // H.264 인코더
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @NotNull
    private H264Encoder h264Encoder;

    // 출력 화질(해상도 상한)
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    @NotNull
    private TranscodingQuality quality;

    @Builder
    public TranscodingSetting(
            final HwAccel hwAccel,
            final H264Encoder h264Encoder,
            final TranscodingQuality quality
    ) {
        this.id = SINGLETON_ID;
        this.hwAccel = hwAccel;
        this.h264Encoder = h264Encoder;
        this.quality = quality;
    }

    public void update(
            final HwAccel hwAccel,
            final H264Encoder h264Encoder,
            final TranscodingQuality quality
    ) {
        this.hwAccel = hwAccel;
        this.h264Encoder = h264Encoder;
        this.quality = quality;
    }
}