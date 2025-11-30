package com.example.demo.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

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
                .claim("tenantId", tenantId)
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

    /**
     * 从JWT中解析租户ID
     */
    public Long getTenantIdFromJWT(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 从claims中获取tenantId
            Object tenantIdObj = claims.get("tenantId");

            if (tenantIdObj == null) {
                return null;  // 如果没有tenantId声明，返回null
            }

            // 处理不同类型的tenantId值
            if (tenantIdObj instanceof Long) {
                return (Long) tenantIdObj;
            } else if (tenantIdObj instanceof Integer) {
                return ((Integer) tenantIdObj).longValue();
            } else if (tenantIdObj instanceof String) {
                try {
                    return Long.valueOf((String) tenantIdObj);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid tenantId format in token: " + tenantIdObj);
                }
            } else {
                throw new IllegalArgumentException("Unsupported tenantId type in token: " + tenantIdObj.getClass().getName());
            }

        } catch (Exception e) {
            // 记录错误日志但不抛出异常，因为可能在其他验证方法中已经处理过
            System.err.println("Failed to get tenantId from JWT: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从JWT中解析角色列表
     */
    public List<String> getRolesFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(jwtSecret)
                .setSigningKey(getSigningKey())  // 修复：使用getSigningKey()而不是jwtSecret
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("roles", List.class);
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