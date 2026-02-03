package com.seohamin.fshs.v2.global.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${spring.profiles.active}")
    private String ACTIVE_PROFILE;

    @Bean
    protected SecurityFilterChain configure(final HttpSecurity httpSecurity) throws Exception {

        // 배포 환경에서는 CSRF 활성
        if ("prod".equals(ACTIVE_PROFILE)) {
            httpSecurity.csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            );
        } else {
            httpSecurity.csrf(AbstractHttpConfigurer::disable);
            httpSecurity.headers(headers -> headers
                    .frameOptions(frameOptions -> frameOptions.sameOrigin())
            );
            httpSecurity.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/h2-console").permitAll());
        }

        httpSecurity
                .httpBasic(AbstractHttpConfigurer::disable)

                // 세션 로그인 / 로그아웃 API 설정
                .formLogin(form -> form
                        // 로그인 엔드포인트
                        .loginProcessingUrl("/api/v2/auth/login")
                        .successHandler((request, response, authentication) -> {
                            // 성공시 200 응답
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        })
                        .failureHandler((request, response, exception) -> {
                            // 실패시 401 응답
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                )
                .logout(logout -> logout
                        // 로그아웃 엔드포인트
                        .logoutUrl("/api/v2/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // 성공시 200 응답
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        })

                        // 세션 삭제
                        .invalidateHttpSession(true)
                        // 쿠키 삭제
                        .deleteCookies("JSESSIONID")
                )

                // 세션 설정
                .sessionManagement(session -> session
                        // 로그인 성공시 새로운 새션 만들기
                        .sessionFixation().changeSessionId()

                        // 필요할 때만 세션 생성
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)

                        // 중복 로그인 방지 (세션 1개만 허용, 새 로그인이면 기존 로그인 만료)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )

                // 인증 필요 없는 엔드포인트 설정
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                        // 프론트 정적 리소스 허용
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/static/**",
                                "/assets/**",
                                "/favicon.ico"
                        ).permitAll()

                        // 인증이 필요 없는 API
                        .requestMatchers(
                                "/api/swagger-ui/**",
                                "/api/swagger",
                                "/api/v2/auth/**"
                        ).permitAll()

                        // 인증 필요한 API
                        .requestMatchers("/api/**").authenticated()

                        // 나머지 모든 경로는 인증 X (SPA 대응)
                        .anyRequest().permitAll()
                )

                // 인증 안된 사용자 접근시 리다이렉트가 아닌 401 반환
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                );

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
