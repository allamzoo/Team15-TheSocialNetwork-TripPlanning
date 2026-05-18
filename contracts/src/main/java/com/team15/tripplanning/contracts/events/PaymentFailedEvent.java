package com.team15.tripplanning.contracts.events;

// Published by booking-service after POST /api/bookings/settlement/process rejects the settlement
// exchange: payment.events | routing-key: payment.failed
// Consumed by itinerary-service → triggers compensation (publishes itinerary.cancelled)
public record PaymentFailedEvent(Long settlementId, Long itineraryId, String reason) {}
