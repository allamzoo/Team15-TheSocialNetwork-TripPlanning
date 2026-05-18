package com.team15.tripplanning.contracts.events;

// Published by destination-service after S2-F7 rating submission
// exchange: destination.events | routing-key: destination.rated
public record DestinationRatedEvent(Long destinationId, Long itineraryId, Double rating, Long userId) {}
