package org.iptime.raspinas.FSHS.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(){
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(final CorsRegistry corsRegistry){
                corsRegistry.addMapping("/api/v1/streaming-video/**").allowedOriginPatterns("*")
                        .allowedMethods("GET","OPTIONS");
//                        .allowedHeaders("Origin", "Content-Type", "Accept")
//                        .allowCredentials(true).maxAge(3600);

                corsRegistry.addMapping("/api/v1/streaming-audio/**").allowedOriginPatterns("*")
                        .allowedMethods("GET","OPTIONS");

                corsRegistry.addMapping("/api/v1/files/**").allowedOriginPatterns("*")
                        .allowedMethods("GET","POST","PATCH","DELETE","OPTION")
                        .exposedHeaders("Content-Disposition");

                corsRegistry.addMapping("/**").allowedOriginPatterns("*")
                        .allowedMethods("GET","POST","PATCH","DELETE","OPTION");
            }
        };
    }
}
