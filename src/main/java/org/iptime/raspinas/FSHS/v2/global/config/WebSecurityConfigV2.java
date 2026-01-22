package org.iptime.raspinas.FSHS.v2.global.config;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.v2.global.auth.filter.JwtAuthenticationFilterV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
@RequiredArgsConstructor
public class WebSecurityConfigV2 {

    private final CorsConfigV2 corsConfig;
    private final JwtAuthenticationFilterV2 jwtAuthenticationFilter;

    @Bean
    @Order(1)
    protected SecurityFilterChain configureV2(final HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .securityMatcher("/api/v2/**")
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSourceV2()))
                .csrf((csrf) -> csrf.disable())
                .httpBasic((httpBasic) -> httpBasic.disable())
                .securityContext(c -> c.securityContextRepository(new NullSecurityContextRepository())) //http에서 세션 생성 방지
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                        .requestMatchers(
                                "/api/v2/auth/**"
                        ).permitAll()

                        .anyRequest().authenticated());

        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }
}
