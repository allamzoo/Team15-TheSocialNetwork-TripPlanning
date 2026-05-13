package com.team15.tripplanning.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Reactive JWT Global Filter — runs before any route handler (order = -1).
 *
 * Responsibilities:
 *  1. Pass through public paths (/api/auth/**, 5 health endpoints) without token check.
 *  2. For all other paths, parse and validate Bearer token using the shared JWT_SECRET.
 *  3. On success: forward X-User-Id (uid claim), X-User-Role (role claim),
 *     X-Correlation-ID (from request or newly generated) to the downstream service.
 *  4. On failure: return 401 without invoking the route.
 *
 * NOTE: This is a WebFlux GlobalFilter — NOT a Servlet OncePerRequestFilter.
 * The M2 JwtAuthenticationFilter (Servlet) lives inside each service for defense-in-depth.
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtGatewayFilter.class);

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/users/health",
            "/api/destinations/health",
            "/api/itineraries/health",
            "/api/activities/health",
            "/api/bookings/health"
    );

    private final JwtTokenValidator jwtTokenValidator;

    public JwtGatewayFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Resolve or generate correlation ID for every request (public + protected)
        String correlationId = request.getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;

        // Public paths bypass JWT validation
        if (isPublicPath(path)) {
            ServerHttpRequest mutated = request.mutate()
                    .header("X-Correlation-ID", finalCorrelationId)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        // Require Authorization: Bearer <token>
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            JwtTokenValidator.JwtClaims claims = jwtTokenValidator.validate(token);

            // Forward parsed claims + correlation ID downstream
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", String.valueOf(claims.userId()))
                    .header("X-User-Role", claims.role())
                    .header("X-Correlation-ID", finalCorrelationId)
                    .build();

            log.debug("JWT validated: userId={} role={} path={} correlationId={}",
                    claims.userId(), claims.role(), path, finalCorrelationId);

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path={}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1; // Must run before all route handlers
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
