package com.team15.tripplanning.contracts.dto;

import java.util.List;

/**
 * Request body for POST /api/bookings/aggregate-by-itineraries (booking-service).
 * Used by itinerary-service to aggregate booking revenue across a set of itineraries (S2-F3 chain).
 */
public record BookingAggregateRequest(
        List<Long> itineraryIds,
        String startDate,
        String endDate,
        String status
) {}
