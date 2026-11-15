package com.tripagora.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JWTUtil {
    // HS256 서명 키의 JCA 알고리즘 이름
    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKey secretKey;
    private final int accessTokenExpiration;
    private final int refreshTokenExpiration;

    public JWTUtil(
            @Value("${jwt.secretKey}") String secretKey,
            @Value("${jwt.access-token-expiration}") int accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") int refreshTokenExpiration) {
        this.secretKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createToken(Long userId, String role, TokenType type) {
        long now = System.currentTimeMillis();
        long expirationMinutes = type == TokenType.ACCESS ? accessTokenExpiration : refreshTokenExpiration;

        // 고유 ID(jti): 같은 초에 같은 사용자에게 다시 발급해도 토큰이 겹치지 않게 한다 (리프레시 토큰은 DB에서 unique)
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim("userId", userId)
                .claim("role", role)
                .claim("type", type.claimValue())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMinutes * 60 * 1000L))
                .signWith(secretKey)
                .compact();
    }

    // 서명·만료·종류를 한 번에 확인하고 사용자 id와 역할을 꺼낸다. 하나라도 맞지 않으면 빈 값
    public Optional<TokenClaims> parse(String token, TokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.claimValue().equals(claims.get("type", String.class))) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(claims.get("userId", Long.class), claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
