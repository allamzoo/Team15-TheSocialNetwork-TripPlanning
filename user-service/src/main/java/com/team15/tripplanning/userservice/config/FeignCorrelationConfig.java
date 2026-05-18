package com.team15.tripplanning.userservice.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignCorrelationConfig {

    @Bean
    public RequestInterceptor feignCorrelationInterceptor() {
        return template -> {
            // ── 1. X-Correlation-ID from MDC ─────────────────────────────────
            String correlationId = MDC.get("correlationId");
            if (correlationId != null && !correlationId.isBlank()) {
                template.header("X-Correlation-ID", correlationId);
            }

            // ── 2. X-User-Id and X-User-Role from inbound request ────────────
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var request = attrs.getRequest();

                String userId = request.getHeader("X-User-Id");
                if (userId != null && !userId.isBlank()) {
                    template.header("X-User-Id", userId);
                }

                String userRole = request.getHeader("X-User-Role");
                if (userRole != null && !userRole.isBlank()) {
                    template.header("X-User-Role", userRole);
                }
            }
        };
    }
}
