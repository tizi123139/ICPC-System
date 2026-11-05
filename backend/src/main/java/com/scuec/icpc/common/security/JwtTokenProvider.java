package com.scuec.icpc.common.security;

import com.scuec.icpc.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = properties.getExpirationSeconds();
    }

    public String createToken(UserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.getStudentId())
                .claim("uid", principal.getMemberId())
                .claim("name", principal.getName())
                .claim("roles", principal.getRoles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    /**
     * 计算 Token 剩余有效秒数，用于登出时写入 Redis 黑名单。
     */
    public long remainingSeconds(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return expirationSeconds;
        }
        return Math.max(0, (expiration.getTime() - System.currentTimeMillis()) / 1000);
    }

    @SuppressWarnings("unchecked")
    public List<String> rolesOf(Claims claims) {
        Object value = claims.get("roles");
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public Long memberIdOf(Claims claims) {
        Object value = claims.get("uid");
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }
}
