package com.team15.tripplanning.bookingservice.messaging.publisher;

import java.math.BigDecimal;

/**
 * S5-EVENTS — Contract for publishing payment saga events to payment.events TopicExchange.
 *
 * Routing keys:
 *   payment.initiated  — settlement row created (PENDING); saga started
 *   payment.completed  — settlement succeeded (COMPLETED)
 *   payment.failed     — settlement rejected (FAILED)
 *   payment.refunded   — itinerary cancelled; CONFIRMED bookings refunded
 */
public interface PaymentEventPublisher {

    void publishPaymentInitiated(Long settlementId, Long itineraryId, BigDecimal amount);

    void publishPaymentCompleted(Long settlementId, Long itineraryId, BigDecimal amount);

    void publishPaymentFailed(Long settlementId, Long itineraryId, String reason);

    void publishPaymentRefunded(Long settlementId, Long itineraryId, BigDecimal refundAmount);
}
