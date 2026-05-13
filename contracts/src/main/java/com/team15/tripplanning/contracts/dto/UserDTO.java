package com.team15.tripplanning.contracts.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Returned by user-service GET /api/users/{id} (existing M1 CRUD endpoint).
 * Password field is excluded (M2 §4.1 security rule preserved).
 * Used by S2-F8 (ADMIN check), S3-F4 (saga pre-check), S3-F11, S3-F12, S5-F3.
 */
public record UserDTO(
        Long id,
        String name,
        String email,
        String phone,
        String role,
        String status,
        Map<String, Object> preferences,
        LocalDateTime createdAt
) {}
