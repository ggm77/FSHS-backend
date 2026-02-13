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

    @Bean
    public CommandLineRunner hardwareDetector() {
        return args -> {
            log.info("[FFmpeg로 하드웨어 가속기 탐색 중...]");

            try {
                // 1) ProcessBuilder로 하드웨어 가속기 찾는 명령어 빌드
                final ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-hwaccels");
                pb.redirectErrorStream(true);

                // 2) 명령어 실행
                final Process p = pb.start();

                // 3) 출력값 읽어오기
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean headerFound = false;

                    // 3-1) 읽어올 값 없을 때 까지 반복
                    while ((line = reader.readLine()) != null) {
                        // Hardware acceleration methods: -> 이후로 읽기 시작
                        if (line.contains("Hardware acceleration methods:")) {
                            headerFound = true;
                            continue;
                        }
                        // 하드웨어 가속기 정보가 존재하면 집합에 추가
                        if (headerFound && !line.isBlank()) {
                            supportedAccelerators.add(line.trim());
                        }
                    }
                }
                // 타임아웃 확인
                if (!p.waitFor(10, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                    log.warn("[하드웨어 가속기 탐색 강제 종료] - 타임아웃");
                }
                else {
                    log.info("[하드웨어 가속기 탐색 완료] - 지원 목록: {}", supportedAccelerators);
                }
            } catch (final Exception ex) {
                log.error("[하드웨어 가속기 탐색 중 에러 발생]", ex);
            }
        };
    }
}