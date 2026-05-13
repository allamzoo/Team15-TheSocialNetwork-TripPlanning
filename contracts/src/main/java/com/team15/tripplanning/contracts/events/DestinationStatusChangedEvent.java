package com.team15.tripplanning.contracts.events;

// Published by destination-service after S2-F4 updates destination status
// exchange: destination.events | routing-key: destination.status-changed
public record DestinationStatusChangedEvent(Long destinationId, String oldStatus, String newStatus) {}
