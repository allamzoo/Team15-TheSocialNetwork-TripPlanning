package com.team15.tripplanning.contracts.events;

// Published by activity-service after consuming itinerary.cancelled and tombstoning the Activity
public record ActivityCancelledEvent(Long activityId, Long itineraryId) {}
