package org.iptime.raspinas.FSHS;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@EnableAsync
@SpringBootApplication
public class FshsApplication {
	// 2023.11.06 develop start
	public static void main(String[] args) {
		SpringApplication.run(FshsApplication.class, args);
	}

}
