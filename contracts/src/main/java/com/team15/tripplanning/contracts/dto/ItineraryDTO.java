package com.team15.tripplanning.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Returned by itinerary-service GET /api/itineraries/{itineraryId} (existing M1 CRUD endpoint).
 * Used by S2-F7, S4-F2/F4, S5-F4, S5-F12, and saga consumers.
 */
public record ItineraryDTO(
        Long id,
        Long userId,
        Long destinationId,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal estimatedBudget,
        Map<String, Object> metadata
) {}
