package com.seohamin.fshs.v2.global.init;

import com.seohamin.fshs.v2.global.config.FfmpegConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Order(2)
@Slf4j
public class FfmpegHardwareDetector implements CommandLineRunner {

    private final FfmpegConfig ffmpegConfig;

    @Override
    public void run(String... args) {
        log.info("[FFmpeg로 하드웨어 가속기 탐색 중...]");

        // 1) ProcessBuilder로 하드웨어 가속기 찾는 명령어 빌드
        final ProcessBuilder pb = new ProcessBuilder(ffmpegConfig.getFfmpeg(), "-hwaccels");
        pb.redirectErrorStream(true);

        try {
            // 2) 명령어 실행
            final Process p = pb.start();

            // 3) 출력값 읽어오기
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                List<String> lines = reader.lines().toList();
                boolean headerFound = false;

                // 3-1) 읽어온 값 분석
                for (String line : lines) {
                    // Hardware acceleration methods: -> 이후로 읽기 시작
                    if (line.contains("Hardware acceleration methods:")) {
                        headerFound = true;
                        continue;
                    }
                    // 하드웨어 가속기 정보가 존재하면 집합에 추가
                    if (headerFound && !line.isBlank()) {
                        ffmpegConfig.addAccelerator(line.trim());
                    }
                }
            }
            // 타임아웃 확인
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.warn("[하드웨어 가속기 탐색 강제 종료] - 타임아웃");
            } else {
                log.info("[하드웨어 가속기 탐색 완료] - 지원 목록: {}", ffmpegConfig.getSupportedAccelerators());
            }
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[하드웨어 가속기 탐색 중 에러 발생] 시스템 인터럽트 발생");
        } catch (final Exception ex) {
            log.error("[하드웨어 가속기 탐색 중 에러 발생]", ex);
        }
    }
}
