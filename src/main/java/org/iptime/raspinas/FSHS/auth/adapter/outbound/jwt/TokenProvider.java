package org.iptime.raspinas.FSHS.auth.adapter.outbound.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.iptime.raspinas.FSHS.common.exception.CustomException;
import org.iptime.raspinas.FSHS.common.exception.constants.ExceptionCode;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class TokenProvider {

    private static final Key SECURITY_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    public String getTokenType(){
        return "bearer";
    }

    public String createAccessToken(final Long id){

        //Access token expired date set 1 hour.
        final Date exprTime = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));

        final String userId = id.toString();

        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, SECURITY_KEY)
                .setSubject(userId).setIssuedAt(new Date()).setExpiration(exprTime)
                .compact();
    }

    public String createRefreshToken(final Long id){

        //Refresh token expired date set 2 week.
        final Date exprTime = Date.from(Instant.now().plus(14, ChronoUnit.DAYS));

        final String userId = id.toString();

        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, SECURITY_KEY)
                .setSubject(userId).setIssuedAt(new Date()).setExpiration(exprTime)
                .compact();
    }

    public String validate(final String token){
        try{
            final Claims claims = Jwts.parserBuilder().setSigningKey(SECURITY_KEY).build().parseClaimsJws(token).getBody();
            return claims.getSubject();

        } catch (Exception ex){
            throw new CustomException(ExceptionCode.TOKEN_NOT_VALID);
        }

    }
}
