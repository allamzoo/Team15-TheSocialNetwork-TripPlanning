package com.team15.tripplanning.contracts.dto;

import java.util.Map;

/**
 * Returned by destination-service GET /api/destinations/{id} (existing M1 CRUD endpoint).
 * Used by itinerary-service S3-F2, S3-F12, booking-service S5-F4, activity-service S4-F2/F4.
 */
public record DestinationDTO(
        Long id,
        String name,
        String country,
        String category,
        String status,
        Double rating,
        Integer totalRatings,
        Map<String, Object> details
) {}
