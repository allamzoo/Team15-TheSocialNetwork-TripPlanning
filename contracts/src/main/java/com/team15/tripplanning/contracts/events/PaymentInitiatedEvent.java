package com.team15.tripplanning.contracts.events;

import java.math.BigDecimal;

// Published by booking-service after consuming itinerary.completed and inserting SETTLEMENT_PENDING row
// exchange: payment.events | routing-key: payment.initiated
public record PaymentInitiatedEvent(Long settlementId, Long itineraryId, BigDecimal amount) {}
