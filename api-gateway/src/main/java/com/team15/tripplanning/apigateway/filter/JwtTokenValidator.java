package com.team15.tripplanning.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
public class JwtTokenValidator {

    private final SecretKey secretKey;

    public JwtTokenValidator(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtClaims validate(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = claims.get("uid", Long.class);
        if (userId == null) {
            Number uid = claims.get("uid", Number.class);
            userId = uid != null ? uid.longValue() : null;
        }

        String role = claims.get("role", String.class);

        if (userId == null || role == null || role.isBlank()) {
            throw new IllegalArgumentException("Missing required JWT claims");
        }

        return new JwtClaims(userId, role);
    }

    public record JwtClaims(Long userId, String role) {}
}