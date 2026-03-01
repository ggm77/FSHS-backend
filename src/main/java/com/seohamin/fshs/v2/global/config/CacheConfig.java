package com.seohamin.fshs.v2.global.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
}
