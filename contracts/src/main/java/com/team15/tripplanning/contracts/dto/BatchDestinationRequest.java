package com.team15.tripplanning.contracts.dto;

import java.util.List;

/**
 * Request body for POST /api/destinations/batch (destination-service).
 * Used by booking-service S5-F10 to fold N destination lookups into 1 Feign call.
 */
public record BatchDestinationRequest(List<Long> destinationIds) {}
