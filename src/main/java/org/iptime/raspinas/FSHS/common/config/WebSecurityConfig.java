package org.iptime.raspinas.FSHS.common.config;

import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.auth.adapter.inbound.security.JwtAuthenticationFilter;
import org.iptime.raspinas.FSHS.auth.application.service.PrincipalOauth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PrincipalOauth2UserService principalOauth2UserService;

    @Bean
    protected SecurityFilterChain configure(final HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf((csrf) -> csrf.disable())
                .httpBasic((httpBasic) -> httpBasic.disable())
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth -> oauth.loginPage("/login").defaultSuccessUrl("/").userInfoEndpoint(userInfo -> userInfo.userService(principalOauth2UserService)))
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests.requestMatchers(
                        "/api/v1/auth/**",
                        "/api/v1/refresh-token",
                        "/api/v1/streaming-thumbnail/**",
                        "/api/v1/streaming-video/**",
                        "/api/v1/streaming-audio/**",
                        "/api/v1/test/**",
                        "/api/swagger/**"
                        ).permitAll().anyRequest().authenticated());

        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder getPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
