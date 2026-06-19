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

    @Getter
    @Setter
    private String selectedHwAccelApi = "none";

    @Getter
    @Setter
    private String selectedH264Encoder = "libx264";

    // 실시간 트랜스코딩 출력 해상도 상한 (화질 설정에서 갱신됨)
    @Getter
    @Setter
    private int transcodingWidth = 1920;

    @Getter
    @Setter
    private int transcodingHeight = 1080;
}