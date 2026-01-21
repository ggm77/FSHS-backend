package org.iptime.raspinas.FSHS.v1.global.config;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FfmpegBeanConfig {
    @Value("${ffmpeg.path.mpeg}")
    private String mpeg;

    @Value("${ffmpeg.path.probe}")
    private String probe;

    @Bean
    public FFmpeg ffmpeg() throws IOException {
        return new FFmpeg(mpeg);
    }

    @Bean
    public FFprobe ffprobe() throws IOException {
        return new FFprobe(probe);
    }
}
