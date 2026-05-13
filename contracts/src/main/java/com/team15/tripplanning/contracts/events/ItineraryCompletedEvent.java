package com.team15.tripplanning.contracts.events;

import java.math.BigDecimal;

// Published by itinerary-service after S3-F4 saga trigger (status → COMPLETING)
public record ItineraryCompletedEvent(Long itineraryId, Long userId, Long destinationId, BigDecimal totalAmount) {}
