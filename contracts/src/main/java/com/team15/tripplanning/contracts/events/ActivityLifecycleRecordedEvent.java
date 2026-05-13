package com.team15.tripplanning.contracts.events;

// Published by activity-service after recording a lifecycle event in Cassandra (M2 S4-F11)
public record ActivityLifecycleRecordedEvent(Long activityId, Long itineraryId, String status) {}
