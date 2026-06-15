package com.seohamin.fshs.v2.global.init;

import com.seohamin.fshs.v2.domain.transcoding.service.TranscodingSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(3)
@Slf4j
public class TranscodingSettingInitializer implements CommandLineRunner {

    private final TranscodingSettingService transcodingSettingService;

    @Override
    public void run(String... args) {
        // 시작 시 DB의 트랜스코딩 설정을 FfmpegConfig에 반영 (없으면 기본값으로 생성)
        transcodingSettingService.loadAndApply();
        log.info("[트랜스코딩 설정 로드 완료]");
    }
}