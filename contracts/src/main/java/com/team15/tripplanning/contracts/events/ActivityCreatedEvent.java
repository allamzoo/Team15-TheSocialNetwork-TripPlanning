package com.team15.tripplanning.contracts.events;

// Published by activity-service after S4-F2 / S4-F4 successfully creates an Activity row
public record ActivityCreatedEvent(Long activityId, Long itineraryId, String category) {}
