package org.iptime.raspinas.FSHS.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenProvider {

    private static final Key SECURITY_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    public String getTokenType(){
        return "bearer";
    }

    public String createAccessToken(Long id){
        Date exprTime = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));//Refresh token expired date set 1 hour.
        String userId = id.toString();
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, SECURITY_KEY)
                .setSubject(userId).setIssuedAt(new Date()).setExpiration(exprTime)
                .compact();
    }

    public String createRefreshToken(Long id){
        Date exprTime = Date.from(Instant.now().plus(14, ChronoUnit.DAYS));//Refresh token expired date set 2 week.
        String userId = id.toString();
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, SECURITY_KEY)
                .setSubject(userId).setIssuedAt(new Date()).setExpiration(exprTime)
                .compact();
    }

    public String validate(String token){
        try{
            Claims claims = Jwts.parserBuilder().setSigningKey(SECURITY_KEY).build().parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (Exception e){
            throw new CustomException(ExceptionCode.TOKEN_NOT_VALID);
        }

    }
}
