package com.seohamin.fshs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FshsApplication {
	// 2023.11.06 develop start
	// 2026.01.22 v2 develop start
	public static void main(String[] args) {
		SpringApplication.run(FshsApplication.class, args);
	}

}
