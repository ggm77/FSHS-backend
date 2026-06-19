package com.seohamin.fshs.v2.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${spring.profiles.active}")
    private String ACTIVE_PROFILE;

    @Value("${remember-me.key:dev-insecure-default-key}")
    private String rememberMeKey;

    @Bean
    public PersistentTokenRepository persistentTokenRepository(final DataSource dataSource, final JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS persistent_logins (
                    username VARCHAR(64) NOT NULL,
                    series   VARCHAR(64) PRIMARY KEY,
                    token    VARCHAR(64) NOT NULL,
                    last_used DATETIME NOT NULL
                )
                """);
        final JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }

    @Bean
    protected SecurityFilterChain configure(final HttpSecurity httpSecurity, final PersistentTokenRepository persistentTokenRepository) throws Exception {

        // 배포 환경에서는 CSRF 활성
        if ("prod".equals(ACTIVE_PROFILE)) {
            final CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

            httpSecurity.csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers("/api/v2/auth/login")
            );
            httpSecurity.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        } else {
            httpSecurity.csrf(AbstractHttpConfigurer::disable);
        }

        httpSecurity
                .cors(Customizer.withDefaults())
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
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeParameter("remember-me")
                        .tokenRepository(persistentTokenRepository)
                        .tokenValiditySeconds(30 * 24 * 60 * 60) // 30일
                        .key(rememberMeKey)
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
                        .deleteCookies("JSESSIONID", "remember-me")
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

    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
                throws ServletException, IOException {
            final CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
