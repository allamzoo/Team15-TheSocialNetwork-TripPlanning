package com.team15.tripplanning.bookingservice.messaging.publisher;

import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import com.team15.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Real RabbitMQ-backed publisher active when m3.messaging.enabled=true.
 * Publishes to the payment.events TopicExchange declared in AmqpConfig.
 */
@Component("rabbitPaymentEventPublisher")
@ConditionalOnProperty(name = "m3.messaging.enabled", havingValue = "true")
public class RabbitPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitPaymentEventPublisher.class);
    private static final String EXCHANGE = "payment.events";

    private final RabbitTemplate rabbitTemplate;

    public RabbitPaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishPaymentInitiated(Long settlementId, Long itineraryId, BigDecimal amount) {
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(settlementId, itineraryId, amount);
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.initiated", event);
        log.info("Published payment.initiated — settlementId={} itineraryId={} amount={}",
                settlementId, itineraryId, amount);
    }

    @Override
    public void publishPaymentCompleted(Long settlementId, Long itineraryId, BigDecimal amount) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(settlementId, itineraryId, amount);
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.completed", event);
        log.info("Published payment.completed — settlementId={} itineraryId={} amount={}",
                settlementId, itineraryId, amount);
    }

    @Override
    public void publishPaymentFailed(Long settlementId, Long itineraryId, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(settlementId, itineraryId, reason);
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.failed", event);
        log.info("Published payment.failed — settlementId={} itineraryId={} reason={}",
                settlementId, itineraryId, reason);
    }

    @Override
    public void publishPaymentRefunded(Long settlementId, Long itineraryId, BigDecimal refundAmount) {
        PaymentRefundedEvent event = new PaymentRefundedEvent(settlementId, itineraryId, refundAmount);
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.refunded", event);
        log.info("Published payment.refunded — settlementId={} itineraryId={} refundAmount={}",
                settlementId, itineraryId, refundAmount);
    }
}
