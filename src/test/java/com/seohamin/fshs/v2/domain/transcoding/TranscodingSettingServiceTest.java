package com.seohamin.fshs.v2.domain.transcoding;

import com.seohamin.fshs.v2.domain.transcoding.dto.TranscodingSettingResponseDto;
import com.seohamin.fshs.v2.domain.transcoding.dto.TranscodingSettingUpdateRequestDto;
import com.seohamin.fshs.v2.domain.transcoding.entity.H264Encoder;
import com.seohamin.fshs.v2.domain.transcoding.entity.HwAccel;
import com.seohamin.fshs.v2.domain.transcoding.entity.TranscodingQuality;
import com.seohamin.fshs.v2.domain.transcoding.entity.TranscodingSetting;
import com.seohamin.fshs.v2.domain.transcoding.repository.TranscodingSettingRepository;
import com.seohamin.fshs.v2.domain.transcoding.service.TranscodingSettingService;
import com.seohamin.fshs.v2.domain.user.entity.Role;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.config.FfmpegConfig;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TranscodingSettingServiceTest {

    @Mock
    private TranscodingSettingRepository transcodingSettingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FfmpegConfig ffmpegConfig;

    @InjectMocks
    private TranscodingSettingService transcodingSettingService;

    private User userWithRole(final String username, final Role role) {
        final User user = User.builder().username(username).password("pw").build();
        user.updateRole(role);
        return user;
    }

    private TranscodingSetting defaultSetting() {
        return TranscodingSetting.builder()
                .hwAccel(HwAccel.CUDA)
                .h264Encoder(H264Encoder.H264_NVENC)
                .quality(TranscodingQuality.P1080)
                .build();
    }

    @Test
    @DisplayName("설정 변경 : 어드민이면 DB 갱신 + FfmpegConfig에 즉시 반영")
    void updateSettings_admin_appliesToConfig() {
        // Given
        given(userRepository.findByUsername("admin"))
                .willReturn(Optional.of(userWithRole("admin", Role.ADMIN)));
        given(transcodingSettingRepository.findById(TranscodingSetting.SINGLETON_ID))
                .willReturn(Optional.of(defaultSetting()));
        final TranscodingSettingUpdateRequestDto request = new TranscodingSettingUpdateRequestDto(
                HwAccel.NONE, H264Encoder.LIBX264, TranscodingQuality.P720);

        // When
        final TranscodingSettingResponseDto response =
                transcodingSettingService.updateSettings(request, "admin");

        // Then
        assertThat(response.hwAccel()).isEqualTo(HwAccel.NONE);
        assertThat(response.h264Encoder()).isEqualTo(H264Encoder.LIBX264);
        assertThat(response.quality()).isEqualTo(TranscodingQuality.P720);
        then(ffmpegConfig).should().setSelectedHwAccelApi("");
        then(ffmpegConfig).should().setSelectedH264Encoder("libx264");
        then(ffmpegConfig).should().setTranscodingWidth(1280);
        then(ffmpegConfig).should().setTranscodingHeight(720);
    }

    @Test
    @DisplayName("설정 변경 : 어드민이 아니면 ACCESS_DENIED")
    void updateSettings_nonAdmin_throws() {
        // Given
        given(userRepository.findByUsername("user"))
                .willReturn(Optional.of(userWithRole("user", Role.USER)));
        final TranscodingSettingUpdateRequestDto request = new TranscodingSettingUpdateRequestDto(
                HwAccel.NONE, H264Encoder.LIBX264, TranscodingQuality.P720);

        // When & Then
        assertThatThrownBy(() -> transcodingSettingService.updateSettings(request, "user"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("설정 조회 : 어드민이 아니면 ACCESS_DENIED")
    void getSettings_nonAdmin_throws() {
        // Given
        given(userRepository.findByUsername("user"))
                .willReturn(Optional.of(userWithRole("user", Role.USER)));

        // When & Then
        assertThatThrownBy(() -> transcodingSettingService.getSettings("user"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.ACCESS_DENIED);
    }
}