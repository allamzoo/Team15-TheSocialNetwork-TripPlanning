package com.team15.tripplanning.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates JWT tokens using the shared JWT_SECRET (same secret as all M2 services).
 *
 * This is a Spring @Component (not the Singleton pattern used inside individual services),
 * because the gateway has its own Spring context and @Value injection is available.
 * The validation logic itself is identical to the M2 JwtConfigurationManager Singleton.
 */
@Component
public class JwtTokenValidator {

    private static JwtTokenValidator instance;

    private SecretKey secretKey;

    public JwtTokenValidator(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        instance = this;
    }

    /** Access the singleton after Spring initialises the bean (used by JwtGatewayFilter). */
    public static JwtTokenValidator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("JwtTokenValidator not yet initialised by Spring");
        }
        return instance;
    }

    /**
     * Parses and validates the given JWT token.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return parsed Claims record
     * @throws JwtException / IllegalArgumentException on invalid token
     */
    public JwtClaims validate(String token) {
        Claims body = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = body.get("uid", Long.class);
        if (userId == null) {
            // Some JWT libraries store numbers as Integer
            Number uid = (Number) body.get("uid");
            userId = uid != null ? uid.longValue() : null;
        }
        String role = body.get("role", String.class);

        return new JwtClaims(userId, role);
    }

    /** Immutable record holding the parsed JWT claims forwarded downstream. */
    public record JwtClaims(Long userId, String role) {}
}
