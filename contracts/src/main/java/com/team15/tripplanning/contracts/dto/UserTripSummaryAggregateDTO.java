package com.team15.tripplanning.contracts.dto;

import java.math.BigDecimal;

/**
 * Returned by itinerary-service GET /api/itineraries/user/{userId}/summary (called by S1-F3).
 * completedTrips counts itineraries in STATUS_COMPLETED_FAMILY
 * (COMPLETED, COMPLETING, PAYMENT_PENDING, PAID).
 * totalBudget sums estimatedBudget over the same rows.
 */
public record UserTripSummaryAggregateDTO(
        long totalTrips,
        long completedTrips,
        long cancelledTrips,
        BigDecimal totalBudget,
        BigDecimal averageBudget
) {
    /** Convenience factory for the Feign error-fallback (user not found in itinerary-service). */
    public static UserTripSummaryAggregateDTO empty() {
        return new UserTripSummaryAggregateDTO(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
