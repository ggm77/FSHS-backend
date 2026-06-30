package com.seohamin.fshs.v2.global.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seohamin.fshs.v2.domain.file.entity.Status;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public Cache<Long, String> filePathCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(3))
                .maximumSize(1000)
                .build();
    }

    @Bean
    public Cache<String, Status> fileStatusCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(1000)
                .build();
    }

    // 키: "fileId:username" → 파일 접근 권한 결과 캐시
    @Bean
    public Cache<String, Boolean> fileAccessCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(5000)
                .build();
    }

    @Bean
    public Cache<String, String> sharedFilePathCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(3))
                .maximumSize(1000)
                .build();
    }
}
