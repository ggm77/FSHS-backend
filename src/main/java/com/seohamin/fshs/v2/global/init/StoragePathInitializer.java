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
@Order(3)
@Slf4j
public class StoragePathInitializer implements CommandLineRunner {

    @Value("${fshs.storage.data-path}")
    private String dataPathStr;

    @Value("${fshs.storage.temp-path}")
    private String tempPathStr;

    @Override
    public void run(String... args) {
        log.info("[데이터 폴더와 임시 폴더 확인 중...]");

        createDirectory(dataPathStr, "데이터");
        createDirectory(tempPathStr, "임시");
    }

    private void createDirectory(final String pathStr, final String label) {
        try {
            Files.createDirectories(Path.of(pathStr));
            log.info("[{} 폴더 생성 완료]", label);
        } catch (final IOException ex) {
            throw new RuntimeException("[" + label + " 폴더를 생성할 수 없습니다.] path: " + pathStr, ex);
        }
    }
}
