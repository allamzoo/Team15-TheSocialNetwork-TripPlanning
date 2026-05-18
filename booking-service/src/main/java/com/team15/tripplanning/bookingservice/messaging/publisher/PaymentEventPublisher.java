package com.team15.tripplanning.bookingservice.messaging.publisher;

import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import com.team15.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * S5-EVENTS — Publishes all payment-related events to the payment.events TopicExchange.
 *
 * Routing keys:
 *   payment.initiated  — saga started, settlement row is SETTLEMENT_PENDING
 *   payment.completed  — settlement reached SETTLED
 *   payment.failed     — settlement reached PAYMENT_FAILED
 *   payment.refunded   — itinerary cancelled, CONFIRMED bookings refunded
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Publishing payment.initiated: settlementId={} itineraryId={} amount={}",
                event.settlementId(), event.itineraryId(), event.amount());
        rabbitTemplate.convertAndSend(AmqpConfig.PAYMENT_EXCHANGE, AmqpConfig.ROUTING_PAYMENT_INITIATED, event);
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing payment.completed: settlementId={} itineraryId={} amount={}",
                event.settlementId(), event.itineraryId(), event.amount());
        rabbitTemplate.convertAndSend(AmqpConfig.PAYMENT_EXCHANGE, AmqpConfig.ROUTING_PAYMENT_COMPLETED, event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publishing payment.failed: settlementId={} itineraryId={} reason={}",
                event.settlementId(), event.itineraryId(), event.reason());
        rabbitTemplate.convertAndSend(AmqpConfig.PAYMENT_EXCHANGE, AmqpConfig.ROUTING_PAYMENT_FAILED, event);
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        log.info("Publishing payment.refunded: settlementId={} itineraryId={} refundAmount={}",
                event.settlementId(), event.itineraryId(), event.refundAmount());
        rabbitTemplate.convertAndSend(AmqpConfig.PAYMENT_EXCHANGE, AmqpConfig.ROUTING_PAYMENT_REFUNDED, event);
    }
}
