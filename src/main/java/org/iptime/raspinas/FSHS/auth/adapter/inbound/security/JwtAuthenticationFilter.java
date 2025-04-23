package org.iptime.raspinas.FSHS.auth.adapter.inbound.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iptime.raspinas.FSHS.auth.adapter.outbound.jwt.TokenProvider;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String token = parseBearToken(request);

        try{

            //Null handling to prevent potential NullPointerExceptions. | null 방지
            if(token != null && !token.equalsIgnoreCase("null")){

                final String userId = tokenProvider.validate(token);

                final AbstractAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userId, null, AuthorityUtils.NO_AUTHORITIES);
                final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authenticationToken);
                SecurityContextHolder.setContext(securityContext);
            }

        } catch (Exception ex){
            log.error("JwtAuthenticationFilter.doFilterInternal message:{}",ex.getMessage(),ex);
        }

        filterChain.doFilter(request, response);
    }

    public String parseBearToken(final HttpServletRequest request){

        final String bearerToken = request.getHeader("Authorization");

        //Verify if the provided token is a Bearer token. | Bearer 토큰인지 확인
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer "))
            return bearerToken.substring(7);

        return null;
    }
}
