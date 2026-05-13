package com.team15.tripplanning.contracts.dto;

import java.util.List;

/**
 * Request body for POST /api/itineraries/batch (itinerary-service).
 * Used by booking-service S5-F10 to fold N itinerary lookups into 1 Feign call.
 */
public record BatchItineraryRequest(List<Long> itineraryIds) {}
