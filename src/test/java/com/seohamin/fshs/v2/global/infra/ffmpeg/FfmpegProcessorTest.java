package com.seohamin.fshs.v2.global.infra.ffmpeg;

import com.seohamin.fshs.v2.global.config.FfmpegConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FfmpegProcessorTest {

    private static final String MAX_OUTPUT_RESOLUTION_FILTER =
            "scale=w=1920:h=1080:force_original_aspect_ratio=decrease:force_divisible_by=2";

    private FfmpegProcessor ffmpegProcessor;

    @BeforeEach
    void setUp() {
        final FfmpegConfig ffmpegConfig = new FfmpegConfig();
        ReflectionTestUtils.setField(ffmpegConfig, "ffmpeg", "/usr/bin/ffmpeg");
        ReflectionTestUtils.setField(ffmpegConfig, "ffprobe", "/usr/bin/ffprobe");
        ffmpegConfig.setSelectedHwAccelApi("cuda");
        ffmpegConfig.setSelectedH264Encoder("h264_nvenc");

        ffmpegProcessor = new FfmpegProcessor(ffmpegConfig, null);
    }

    @Test
    @DisplayName("MP4 실시간 스트림 명령은 출력 해상도를 1080p 이하로 제한한다")
    void buildVideoStreamCommand_capsOutputResolutionTo1080p() {
        final List<String> command = ffmpegProcessor.buildVideoStreamCommand("/tmp/input.mkv", 12.5);

        assertThat(command).containsSubsequence("-vf", MAX_OUTPUT_RESOLUTION_FILTER);
        assertThat(command).containsSubsequence("-pix_fmt", "yuv420p");
        assertThat(command).containsSubsequence("-f", "mp4");
        assertThat(command).doesNotContain("-hwaccel_output_format");
    }

    @Test
    @DisplayName("HLS 세그먼트 명령은 출력 해상도를 1080p 이하로 제한한다")
    void buildHlsSegmentCommand_capsOutputResolutionTo1080p() {
        final List<String> command = ffmpegProcessor.buildHlsSegmentCommand(Path.of("/tmp/input.mkv"), 18.0);

        assertThat(command).containsSubsequence("-t", "6", "-vf", MAX_OUTPUT_RESOLUTION_FILTER);
        assertThat(command).containsSubsequence("-pix_fmt", "yuv420p");
        assertThat(command).containsSubsequence("-f", "mpegts");
        assertThat(command).doesNotContain("-hwaccel_output_format");
    }
}
