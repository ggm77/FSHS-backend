package com.seohamin.fshs.v2.domain.transcoding.service;

import com.seohamin.fshs.v2.domain.transcoding.dto.TranscodingSettingResponseDto;
import com.seohamin.fshs.v2.domain.transcoding.dto.TranscodingSettingUpdateRequestDto;
import com.seohamin.fshs.v2.domain.transcoding.entity.H264Encoder;
import com.seohamin.fshs.v2.domain.transcoding.entity.HwAccel;
import com.seohamin.fshs.v2.domain.transcoding.entity.TranscodingQuality;
import com.seohamin.fshs.v2.domain.transcoding.entity.TranscodingSetting;
import com.seohamin.fshs.v2.domain.transcoding.repository.TranscodingSettingRepository;
import com.seohamin.fshs.v2.domain.user.entity.Role;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.config.FfmpegConfig;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TranscodingSettingService {

    private final TranscodingSettingRepository transcodingSettingRepository;
    private final UserRepository userRepository;
    private final FfmpegConfig ffmpegConfig;

    /**
     * 서버 시작 시(초기화 러너에서) DB에 저장된 설정을 FfmpegConfig에 반영한다.
     * 설정이 없으면 현재 FfmpegConfig 기본값(YAML)으로 단일 행을 생성한다.
     */
    @Transactional
    public void loadAndApply() {
        final TranscodingSetting setting = transcodingSettingRepository
                .findById(TranscodingSetting.SINGLETON_ID)
                .orElseGet(() -> transcodingSettingRepository.save(
                        TranscodingSetting.builder()
                                .hwAccel(HwAccel.fromFfmpegValue(ffmpegConfig.getSelectedHwAccelApi()))
                                .h264Encoder(H264Encoder.fromFfmpegValue(ffmpegConfig.getSelectedH264Encoder()))
                                .quality(TranscodingQuality.P1080)
                                .build()
                ));
        applyToFfmpegConfig(setting);
    }

    // 현재 설정 조회 (어드민 전용)
    @Transactional(readOnly = true)
    public TranscodingSettingResponseDto getSettings(final String username) {
        validateAdmin(username);
        return TranscodingSettingResponseDto.of(getSetting());
    }

    // 설정 변경 (어드민 전용) — DB 저장 + 런타임(FfmpegConfig) 반영
    @Transactional
    public TranscodingSettingResponseDto updateSettings(
            final TranscodingSettingUpdateRequestDto request,
            final String username
    ) {
        validateAdmin(username);

        if (request.hwAccel() == null || request.h264Encoder() == null || request.quality() == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final TranscodingSetting setting = getSetting();
        setting.update(request.hwAccel(), request.h264Encoder(), request.quality());
        applyToFfmpegConfig(setting);

        return TranscodingSettingResponseDto.of(setting);
    }

    private TranscodingSetting getSetting() {
        return transcodingSettingRepository.findById(TranscodingSetting.SINGLETON_ID)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_REQUEST));
    }

    // 런타임 트랜스코딩에 즉시 반영되도록 FfmpegConfig 갱신
    private void applyToFfmpegConfig(final TranscodingSetting setting) {
        ffmpegConfig.setSelectedHwAccelApi(setting.getHwAccel().getFfmpegValue());
        ffmpegConfig.setSelectedH264Encoder(setting.getH264Encoder().getFfmpegValue());
        ffmpegConfig.setTranscodingWidth(setting.getQuality().getWidth());
        ffmpegConfig.setTranscodingHeight(setting.getQuality().getHeight());
    }

    // username으로 DB 조회 후 ADMIN 역할 검증
    private void validateAdmin(final String username) {
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
        if (user.getUserRole() != Role.ADMIN) {
            throw new CustomException(ExceptionCode.ACCESS_DENIED);
        }
    }
}