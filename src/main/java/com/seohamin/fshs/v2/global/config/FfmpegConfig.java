package com.seohamin.fshs.v2.global.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class FfmpegConfig {

    @Value("${ffmpeg.ffmpeg}")
    @Getter
    private String ffmpeg;

    @Value("${ffmpeg.ffprobe}")
    @Getter
    private String ffprobe;

    // 하드웨어 가속기 정보를 저장할 장소
    private final Set<String> supportedAccelerators = Collections.synchronizedSet(new HashSet<>());

    /**
     * 외부에서 하드웨어 가속기 정보를 추가하는 메서드
     * @param accelerator 추가할 하드웨어 가속기
     */
    public void addAccelerator(final String accelerator) {
        supportedAccelerators.add(accelerator);
    }

    /**
     * 외부에서 특정 하드웨어 가속기가 사용 가능한지 확인하는 메서드
     * @param accelerator 하드웨어 가속기
     * @return 지원 여부
     */
    public boolean supports(final String accelerator) {
        return supportedAccelerators.contains(accelerator);
    }

    /**
     * 지원하는 하드웨어 가속기 정보 조회하는 메서드
     * @return 지원하는 하드웨어 가속기들
     */
    public Set<String> getSupportedAccelerators() {
        return Collections.unmodifiableSet(supportedAccelerators);
    }
}