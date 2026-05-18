package com.team15.tripplanning.activityservice.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * M3 Section 2.3 — Correlation ID Propagation.
 *
 * Every outgoing Feign request from activity-service automatically gets
 * the X-Correlation-ID header set from whatever MDC.get("correlationId")
 * holds at call time. This value is populated by the JwtAuthenticationFilter
 * which reads the X-Correlation-ID header forwarded by the API Gateway.
 *
 * Result: a single request that triggers a Feign call can be traced
 * end-to-end across activity-service and itinerary-service logs in Loki
 * using the same correlationId value.
 */
@Configuration
public class FeignCorrelationConfig {

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-ID", correlationId);
            }
        };
    }
}