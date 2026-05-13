package com.team15.tripplanning.contracts.dto;

import java.math.BigDecimal;

/**
 * Returned by booking-service GET /api/bookings/itinerary/{itineraryId}/confirmed-summary.
 * Serves two purposes in S3-F4 (saga trigger):
 *  1. Active sub-entity pre-check: count >= 1 means there are CONFIRMED bookings → proceed.
 *  2. Budget calculation: totalRevenue → Itinerary.estimatedBudget is set to this value.
 */
public record ConfirmedSummaryDTO(
        long count,
        BigDecimal totalRevenue
) {}
