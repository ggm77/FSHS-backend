package com.seohamin.fshs.v2.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    /*
     * Jpa Auditing을 활성화 하기 위한 파일
     * 테스트에서도 활성화 하기 위해 따로 뺌
     */
}
