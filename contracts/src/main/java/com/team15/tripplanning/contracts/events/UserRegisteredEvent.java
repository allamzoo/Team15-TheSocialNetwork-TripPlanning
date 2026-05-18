package com.team15.tripplanning.contracts.events;

// Published by user-service after successful registration (S1-F10)
// exchange: user.events | routing-key: user.registered
public record UserRegisteredEvent(Long userId, String email, String role) {}
