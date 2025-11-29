package com.example.demo.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;

//    private SecretKey getSigningKey() {
//        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
//    }

    private SecretKey getSigningKey() {
        // 如果配置了密钥，使用配置的密钥；否则生成一个安全的密钥
        if (jwtSecret != null && !jwtSecret.trim().isEmpty()) {
            // 确保密钥长度足够
            if (jwtSecret.getBytes().length < 32) {
                // 如果密钥太短，进行填充
                byte[] keyBytes = new byte[32];
                byte[] secretBytes = jwtSecret.getBytes();
                System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 32));
                return Keys.hmacShaKeyFor(keyBytes);
            }
            return Keys.hmacShaKeyFor(jwtSecret.getBytes());
        } else {
            // 生成一个安全的密钥（仅用于开发环境）
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    public String generateToken(String username, Long userId, Long tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
//                .claim("tenantId", tenantId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

//    public Long getTenantIdFromJWT(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(getSigningKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//        return claims.get("tenantId", Long.class);
//    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}