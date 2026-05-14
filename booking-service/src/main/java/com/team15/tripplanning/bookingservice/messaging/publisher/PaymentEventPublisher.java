package com.team15.tripplanning.bookingservice.messaging.publisher;

import java.math.BigDecimal;

/**
 * Publishes payment.* AMQP events to the payment.events exchange.
 *
 * S5-READ-DB provides a no-op default bean so the settlement endpoint compiles
 * and runs without RabbitMQ wired up.  S5-EVENTS will replace this with the
 * real RabbitTemplate-backed implementation.
 */
public interface PaymentEventPublisher {

    void publishPaymentCompleted(Long settlementId, Long itineraryId, BigDecimal amount);

    void publishPaymentFailed(Long settlementId, Long itineraryId, String reason);

    void publishPaymentInitiated(Long settlementId, Long itineraryId, BigDecimal amount);

    void publishPaymentRefunded(Long settlementId, Long itineraryId, BigDecimal refundAmount);
}
