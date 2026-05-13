package com.team15.tripplanning.contracts.dto;

/**
 * Returned in the list from destination-service POST /api/destinations/batch.
 * Used by booking-service S5-F10 to fold N Feign calls into 1 when grouping
 * bookings by destination.
 */
public record DestinationSummaryDTO(
        Long destinationId,
        String name,
        String country,
        String category
) {}
