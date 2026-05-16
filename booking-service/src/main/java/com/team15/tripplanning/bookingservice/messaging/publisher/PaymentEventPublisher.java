package com.team15.tripplanning.bookingservice.messaging.publisher;

import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import com.team15.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        rabbitTemplate.convertAndSend(AmqpConfig.PAYMENT_EXCHANGE, "payment.initiated", event);
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        rabbitTemplate.convertAndSend(AmqpConfig.PAYMENT_EXCHANGE, "payment.completed", event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        rabbitTemplate.convertAndSend(AmqpConfig.PAYMENT_EXCHANGE, "payment.failed", event);
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        rabbitTemplate.convertAndSend(AmqpConfig.PAYMENT_EXCHANGE, "payment.refunded", event);
    }
}

