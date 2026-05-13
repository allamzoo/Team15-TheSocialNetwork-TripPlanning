package com.team15.tripplanning.contracts.dto;

/**
 * Returned by itinerary-service
 * GET /api/itineraries/destination/{destinationId}/dashboard-aggregate (called by S2-F12).
 * completedItineraries counts itineraries in STATUS_COMPLETED_FAMILY.
 * totalVisitors counts distinct userId over all itineraries for this destination.
 */
public record DestinationDashboardAggregateDTO(
        long totalItineraries,
        long completedItineraries,
        long totalVisitors
) {}
