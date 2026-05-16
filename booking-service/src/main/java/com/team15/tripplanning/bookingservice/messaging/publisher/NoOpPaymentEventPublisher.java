package com.team15.tripplanning.bookingservice.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Default no-op publisher active while S5-EVENTS hasn't wired RabbitMQ yet.
 * Logs the event at INFO level so tests can see it happened.
 * S5-EVENTS replaces this by providing a @Primary or @ConditionalOnMissingBean-free bean.
 */
@Component
@ConditionalOnMissingBean(name = "rabbitPaymentEventPublisher")
public class NoOpPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpPaymentEventPublisher.class);

    @Override
    public void publishPaymentCompleted(Long settlementId, Long itineraryId, BigDecimal amount) {
        log.info("[NO-OP] payment.completed — settlementId={} itineraryId={} amount={}", settlementId, itineraryId, amount);
    }

    @Override
    public void publishPaymentFailed(Long settlementId, Long itineraryId, String reason) {
        log.info("[NO-OP] payment.failed — settlementId={} itineraryId={} reason={}", settlementId, itineraryId, reason);
    }

    @Override
    public void publishPaymentInitiated(Long settlementId, Long itineraryId, BigDecimal amount) {
        log.info("[NO-OP] payment.initiated — settlementId={} itineraryId={} amount={}", settlementId, itineraryId, amount);
    }

    @Override
    public void publishPaymentRefunded(Long settlementId, Long itineraryId, BigDecimal refundAmount) {
        log.info("[NO-OP] payment.refunded — settlementId={} itineraryId={} refundAmount={}", settlementId, itineraryId, refundAmount);
    }
}
