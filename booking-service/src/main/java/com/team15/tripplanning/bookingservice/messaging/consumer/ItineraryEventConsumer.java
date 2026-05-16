package com.team15.tripplanning.bookingservice.messaging.consumer;

import com.team15.tripplanning.bookingservice.messaging.publisher.AmqpConfig;
import com.team15.tripplanning.bookingservice.saga.SettlementSaga;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * S5-EVENTS — Listens on itinerary.events.booking-consumer for ItineraryCompletedEvent.
 *
 * On receipt:
 *   1. Delegates to SettlementSaga.createPending() which inserts a SETTLEMENT_PENDING row.
 *   2. Saga then publishes payment.initiated automatically.
 *
 * Idempotency is guaranteed by the unique constraint on settlements.itinerary_id:
 * if the same event arrives twice the second call is silently ignored by the saga.
 *
 * The listener is only active when M3_MESSAGING_ENABLED=true (auto-startup=false by default),
 * so tests that don't set this property won't start the listener container.
 */
@Component
public class ItineraryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventConsumer.class);

    private final SettlementSaga settlementSaga;

    public ItineraryEventConsumer(SettlementSaga settlementSaga) {
        this.settlementSaga = settlementSaga;
    }

    @RabbitListener(queues = AmqpConfig.BOOKING_CONSUMER_QUEUE)
    public void onItineraryCompleted(ItineraryCompletedEvent event) {
        log.info("Received itinerary.completed: itineraryId={} userId={} amount={}",
                event.itineraryId(), event.userId(), event.totalAmount());
        settlementSaga.createPending(
                event.itineraryId(),
                event.userId(),
                event.totalAmount());
    }
}
