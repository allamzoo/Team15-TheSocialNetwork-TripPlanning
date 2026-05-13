package com.team15.tripplanning.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway — reactive entry point for all Trip Planning services.
 *
 * IMPORTANT: Do NOT add @EnableFeignClients here. The gateway only routes;
 * it does not call any service via Feign itself.
 *
 * IMPORTANT: Do NOT add spring-boot-starter-web. The gateway is reactive
 * (WebFlux). Mixing Servlet and Reactor contexts breaks startup.
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
