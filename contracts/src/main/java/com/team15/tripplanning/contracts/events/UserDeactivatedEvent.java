package com.team15.tripplanning.contracts.events;

// Published by user-service after S1-F4 successfully sets status = DEACTIVATED
// exchange: user.events | routing-key: user.deactivated
public record UserDeactivatedEvent(Long userId) {}
