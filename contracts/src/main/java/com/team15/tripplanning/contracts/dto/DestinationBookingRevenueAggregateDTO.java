package com.team15.tripplanning.contracts.dto;

import java.math.BigDecimal;

/**
 * Returned by itinerary-service
 * GET /api/itineraries/destination/{destinationId}/booking-revenue?startDate=&endDate=
 * (called by S2-F3 via destination-service → itinerary-service → booking-service chain).
 */
public record DestinationBookingRevenueAggregateDTO(
        long totalBookings,
        BigDecimal totalRevenue,
        BigDecimal averageBookingAmount
) {}
