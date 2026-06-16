package com.seohamin.fshs.v2.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(final String resourcePath, final Resource location) throws IOException {
                        final Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }

                        // /api 하위는 백엔드 API 전용 → 정적 폴백하지 않고 404 처리
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // 나머지 경로는 SPA 진입점(index.html) 서빙
                        return new ClassPathResource("static/index.html");
                    }
                });
    }
}