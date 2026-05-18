package com.team15.tripplanning.contracts.events;

// Published by itinerary-service after S3-F2 assigns a destination (status → PLANNED)
public record ItineraryPlacedEvent(Long itineraryId, Long userId, Long destinationId) {}
