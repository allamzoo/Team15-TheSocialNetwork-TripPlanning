package com.team15.tripplanning.contracts.dto;

import java.math.BigDecimal;

/**
 * Returned by booking-service POST /api/bookings/aggregate-by-itineraries
 * (called by itinerary-service to aggregate booking revenue for a destination — S2-F3 chain).
 * Body: {itineraryIds:[…], startDate, endDate, status}
 */
public record ItineraryAggregateDTO(
        long totalBookings,
        BigDecimal totalRevenue
) {}
