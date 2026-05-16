package com.team15.tripplanning.itineraryservice.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import com.team15.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import com.team15.tripplanning.itineraryservice.config.ItineraryRabbitMQConfig;
import com.team15.tripplanning.itineraryservice.messaging.publisher.ItineraryEventPublisher;
import com.team15.tripplanning.itineraryservice.repository.ItineraryRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ItinerarySagaFeedbackConsumer {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public ItinerarySagaFeedbackConsumer(ItineraryRepository itineraryRepository,
                                         ItineraryEventPublisher eventPublisher,
                                         ObjectMapper objectMapper) {
        this.itineraryRepository = itineraryRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = ItineraryRabbitMQConfig.SAGA_FEEDBACK_QUEUE)
    @Transactional
    public void handlePaymentEvent(Message message,
                                   @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        switch (routingKey) {
            case "payment.initiated"  -> onPaymentInitiated(message);
            case "payment.completed"  -> onPaymentCompleted(message);
            case "payment.failed"     -> onPaymentFailed(message);
            case "payment.refunded"   -> onPaymentRefunded(message);
        }
    }

    // COMPLETING → PAYMENT_PENDING
    private void onPaymentInitiated(Message message) {
        PaymentInitiatedEvent event = parse(message, PaymentInitiatedEvent.class);
        itineraryRepository.transitionStatus(event.itineraryId(), "PAYMENT_PENDING", "COMPLETING");
    }

    // PAYMENT_PENDING → PAID
    private void onPaymentCompleted(Message message) {
        PaymentCompletedEvent event = parse(message, PaymentCompletedEvent.class);
        itineraryRepository.transitionStatus(event.itineraryId(), "PAID", "PAYMENT_PENDING");
    }

    // PAYMENT_PENDING → PAYMENT_FAILED; if rowcount == 1 publish itinerary.cancelled (compensation)
    private void onPaymentFailed(Message message) {
        PaymentFailedEvent event = parse(message, PaymentFailedEvent.class);
        int rowcount = itineraryRepository.transitionStatus(
                event.itineraryId(), "PAYMENT_FAILED", "PAYMENT_PENDING");

        if (rowcount == 1) {
            itineraryRepository.findById(event.itineraryId()).ifPresent(itinerary ->
                eventPublisher.publishCancelled(new ItineraryCancelledEvent(
                        itinerary.getId(),
                        itinerary.getUserId(),
                        itinerary.getDestinationId(),
                        event.reason()
                ))
            );
        }
    }

    // PAYMENT_FAILED → REFUNDED
    private void onPaymentRefunded(Message message) {
        PaymentRefundedEvent event = parse(message, PaymentRefundedEvent.class);
        itineraryRepository.transitionStatus(event.itineraryId(), "REFUNDED", "PAYMENT_FAILED");
    }

    private <T> T parse(Message message, Class<T> type) {
        try {
            return objectMapper.readValue(message.getBody(), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize " + type.getSimpleName(), e);
        }
    }
}
