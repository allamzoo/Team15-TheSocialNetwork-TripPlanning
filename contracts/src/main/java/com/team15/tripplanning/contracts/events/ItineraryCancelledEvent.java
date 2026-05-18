package com.team15.tripplanning.contracts.events;

// Published by itinerary-service after S3-F7 cancel or saga compensation on payment.failed
public record ItineraryCancelledEvent(Long itineraryId, Long userId, Long destinationId, String reason) {}
