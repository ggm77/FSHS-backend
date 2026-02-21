package com.seohamin.fshs.v2.global.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Order(4)
@Slf4j
public class StoragePathInitializer implements CommandLineRunner {

    @Value("${fshs.storage.temp-path}")
    private String tempPathStr;

    @Override
    public void run(String... args) {
        log.info("[임시 폴더와 DB 폴더 확인 중...");

        final Path tempPath = Path.of(tempPathStr);

        try {
            Files.createDirectories(tempPath);
            log.info("[임시 폴더 생성 완료]");
        } catch (final IOException ex) {
            throw new RuntimeException("[임시 폴더를 생성할 수 없습니다.] path: "+tempPathStr, ex);
        }
    }
}
