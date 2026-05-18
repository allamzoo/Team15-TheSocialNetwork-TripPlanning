package com.team15.tripplanning.contracts.dto;

/**
 * Returned in the list from itinerary-service POST /api/itineraries/batch.
 * Used by booking-service S5-F10 to fold N Feign calls into 1 when joining
 * bookings → itinerary → destination.
 */
public record ItinerarySummaryDTO(
        Long itineraryId,
        Long destinationId,
        Long userId,
        String status
) {}
