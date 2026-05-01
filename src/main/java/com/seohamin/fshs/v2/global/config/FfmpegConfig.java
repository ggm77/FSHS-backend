package com.seohamin.fshs.v2.global.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FfmpegConfig {

    @Value("${ffmpeg.ffmpeg}")
    @Getter
    private String ffmpeg;

    @Value("${ffmpeg.ffprobe}")
    @Getter
    private String ffprobe;

    @Value("${ffmpeg.default-hw-accelerator}")
    @Getter
    @Setter
    private String selectedHwAccelApi;

    @Value("${ffmpeg.default-h264-encoder}")
    @Getter
    @Setter
    private String selectedH264Encoder;
}