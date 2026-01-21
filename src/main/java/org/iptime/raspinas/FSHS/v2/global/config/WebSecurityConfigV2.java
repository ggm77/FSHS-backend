package org.iptime.raspinas.FSHS.v2.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
public class WebSecurityConfigV2 {

    @Bean
    @Order(1)
    protected SecurityFilterChain configureV2(final HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .securityMatcher("/api/v2/**")
                .cors(Customizer.withDefaults())
                .csrf((csrf) -> csrf.disable())
                .securityContext(c -> c.securityContextRepository(new NullSecurityContextRepository())) //http에서 세션 생성 방지
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                        .requestMatchers(
                                "/api/v2/user"
                        ).permitAll()

                        .anyRequest().authenticated());

        return httpSecurity.build();
    }
}
