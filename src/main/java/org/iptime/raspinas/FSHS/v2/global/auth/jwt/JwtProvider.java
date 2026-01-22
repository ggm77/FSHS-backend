package org.iptime.raspinas.FSHS.v2.global.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.iptime.raspinas.FSHS.v2.domain.user.entity.Role;
import org.iptime.raspinas.FSHS.v2.global.exception.CustomExceptionV2;
import org.iptime.raspinas.FSHS.v2.global.exception.constants.ExceptionCodeV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtProvider {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    @Value("${jwt.accessToken.exprTime}")
    private Integer ACCESS_TOKEN_EXPIRATION_TIME;

    @Value("${jwt.refreshToken.exprTime}")
    private Integer REFRESH_TOKEN_EXPIRATION_TIME;

    public String getTokenType() {
        return "Bearer";
    }

    public Long getAccessTokenExpirationSeconds() {
        return Long.valueOf(ACCESS_TOKEN_EXPIRATION_TIME);
    }

    /**
     * 액세스 토큰 발급용 메서드
     * @param userId sub로 사용할 유저 ID
     * @param role authorities로 사용할 유저 Role
     * @return 액세스 토큰
     */
    public String createAccessToken(
            final Long userId,
            final Role role
    ) {
        // 시크릿 키 생성
        final SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));

        final String sub = String.valueOf(userId); // sub
        final Instant now = Instant.now(); // 발행 일시
        final Instant exp = now.plusSeconds(ACCESS_TOKEN_EXPIRATION_TIME); // 만료 일시

        // JWT 빌드
        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .setSubject(sub)
                .claim("authorities", List.of(role.getKey()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    /**
     * 리프레시 토큰 발급용 메서드
     * @param userId sub로 사용할 유저 ID
     * @return 리프레시 토큰
     */
    public String createRefreshToken(final Long userId) {
        // 시크릿 키 생성
        final SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));

        final String sub = String.valueOf(userId); // sub
        final Instant now = Instant.now(); // 발행 일시
        final Instant exp = now.plusSeconds(REFRESH_TOKEN_EXPIRATION_TIME); // 만료 일시

        // JWT 빌드
        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .setSubject(sub)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    /**
     * JWT에서 Claims 추출하는 메서드
     * @param token JWT
     * @return Claims
     */
    public Claims getClaims(final String token) {
        // 시크릿 키 생성
        final SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (final JwtException ex) {
            throw new CustomExceptionV2(ExceptionCodeV2.INVALID_TOKEN);
        }
    }

    /**
     * JWT에서 Authorities를 얻는 메서드
     * @param claims JWT의 claims
     * @return JWT의 Authorities
     */
    public List<SimpleGrantedAuthority> getAuthorities(final Claims claims){
        final List<String> roles = claims.get("authorities", List.class);
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
