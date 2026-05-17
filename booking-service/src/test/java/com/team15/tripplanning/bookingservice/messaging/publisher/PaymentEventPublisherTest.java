package com.team15.tripplanning.bookingservice.messaging.publisher;

import static org.mockito.Mockito.verify;

import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import com.team15.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventPublisher — exchange + routing key correctness")
class PaymentEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks PaymentEventPublisher publisher;

    private static final String EXCHANGE = "payment.events";

    @Test
    @DisplayName("payment.initiated → exchange=payment.events, key=payment.initiated")
    void publishInitiated_correctRoutingKey() {
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(1L, 10L, BigDecimal.valueOf(2000));
        publisher.publishPaymentInitiated(event);
        verify(rabbitTemplate).convertAndSend(EXCHANGE, "payment.initiated", event);
    }

    @Test
    @DisplayName("payment.completed → exchange=payment.events, key=payment.completed")
    void publishCompleted_correctRoutingKey() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 10L, BigDecimal.valueOf(2000));
        publisher.publishPaymentCompleted(event);
        verify(rabbitTemplate).convertAndSend(EXCHANGE, "payment.completed", event);
    }

    @Test
    @DisplayName("payment.failed → exchange=payment.events, key=payment.failed")
    void publishFailed_correctRoutingKey() {
        PaymentFailedEvent event = new PaymentFailedEvent(1L, 10L, "Insufficient funds");
        publisher.publishPaymentFailed(event);
        verify(rabbitTemplate).convertAndSend(EXCHANGE, "payment.failed", event);
    }

    @Test
    @DisplayName("payment.refunded → exchange=payment.events, key=payment.refunded")
    void publishRefunded_correctRoutingKey() {
        PaymentRefundedEvent event = new PaymentRefundedEvent(1L, 10L, BigDecimal.valueOf(1500));
        publisher.publishPaymentRefunded(event);
        verify(rabbitTemplate).convertAndSend(EXCHANGE, "payment.refunded", event);
    }

    @Test
    @DisplayName("all four publishers use the same payment.events exchange")
    void allPublishers_useSameExchange() {
        publisher.publishPaymentInitiated(new PaymentInitiatedEvent(1L, 1L, BigDecimal.ONE));
        publisher.publishPaymentCompleted(new PaymentCompletedEvent(1L, 1L, BigDecimal.ONE));
        publisher.publishPaymentFailed(new PaymentFailedEvent(1L, 1L, "err"));
        publisher.publishPaymentRefunded(new PaymentRefundedEvent(1L, 1L, BigDecimal.ONE));

        verify(rabbitTemplate, org.mockito.Mockito.times(4))
                .convertAndSend(org.mockito.ArgumentMatchers.eq(EXCHANGE),
                                org.mockito.ArgumentMatchers.anyString(),
                                (Object) org.mockito.ArgumentMatchers.any());
    }
}