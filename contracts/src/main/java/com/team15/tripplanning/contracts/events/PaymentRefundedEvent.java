package com.team15.tripplanning.contracts.events;

import java.math.BigDecimal;

// Published by booking-service after consuming itinerary.cancelled and refunding CONFIRMED bookings,
// or after S5-F12 (M2 refund flow). exchange: payment.events | routing-key: payment.refunded
public record PaymentRefundedEvent(Long settlementId, Long itineraryId, BigDecimal refundAmount) {}
