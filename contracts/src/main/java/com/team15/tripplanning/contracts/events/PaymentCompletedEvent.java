package com.team15.tripplanning.contracts.events;

import java.math.BigDecimal;

// Published by booking-service after POST /api/bookings/settlement/process settles successfully
// exchange: payment.events | routing-key: payment.completed
public record PaymentCompletedEvent(Long settlementId, Long itineraryId, BigDecimal amount) {}
