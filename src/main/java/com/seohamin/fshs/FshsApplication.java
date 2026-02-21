package com.seohamin.fshs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class FshsApplication {
	// 2023.11.06 develop start
	// 2026.01.22 v2 develop start
	public static void main(String[] args) {

		try {
			Files.createDirectories(Path.of("./db"));
		} catch (final IOException ex) {
			throw new RuntimeException("[FSHS] DB 폴더 생성 중 오류 발생", ex);
		}

		SpringApplication.run(FshsApplication.class, args);
	}

}
