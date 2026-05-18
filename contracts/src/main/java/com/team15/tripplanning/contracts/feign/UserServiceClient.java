package com.team15.tripplanning.contracts.feign;

import com.team15.tripplanning.contracts.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for user-service.
 * URL resolved from application.yml: feign.user-service.url = http://user-service:8080
 *
 * Used by: destination-service (S2-F8 ADMIN check),
 *           itinerary-service (S3-F4 pre-check, S3-F11, S3-F12),
 *           booking-service (S5-F3 existence check, saga consumer).
 */
@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    /**
     * Fetch user by ID. Returns {id, name, email, phone, role, status, preferences, createdAt}.
     * Password field is excluded (M2 §4.1).
     *
     * Callers must wrap in try-catch:
     *  - FeignException.NotFound (404): user does not exist
     *  - FeignException (other): user-service unavailable
     */
    @GetMapping("/api/users/{userId}")
    UserDTO getUser(@PathVariable Long userId);

    /**
     * Internal service-to-service call — no ownership check.
     * Use this from other microservices that need user data without a user JWT.
     */
    @GetMapping("/api/users/{userId}/internal")
    UserDTO getUserInternal(@PathVariable Long userId);
}
