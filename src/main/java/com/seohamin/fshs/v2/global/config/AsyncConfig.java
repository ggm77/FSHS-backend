package com.seohamin.fshs.v2.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);       // 기본적으로 유지할 스레드 개수
        executor.setMaxPoolSize(10);       // 최대 가용 스레드 개수
        executor.setQueueCapacity(500);    // 스레드 풀이 가득 찼을 때 대기할 큐 용량
        executor.setThreadNamePrefix("FSHSv2-Async-");
        executor.initialize();

        return executor;
    }
}
