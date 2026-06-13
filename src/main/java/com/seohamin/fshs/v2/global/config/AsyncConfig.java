package com.seohamin.fshs.v2.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);      // 업로드 분석/이동을 동시에 처리할 스레드 개수
        executor.setMaxPoolSize(10);       // 최대 가용 스레드 개수
        executor.setQueueCapacity(50);     // 대기열을 작게 유지해 과도한 임시 파일 적체를 방지
        executor.setThreadNamePrefix("FSHSv2-Async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();

        return executor;
    }
}
