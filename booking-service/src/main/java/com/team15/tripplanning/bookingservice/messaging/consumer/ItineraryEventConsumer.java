package com.team15.tripplanning.bookingservice.messaging.consumer;

import com.team15.tripplanning.bookingservice.messaging.publisher.AmqpConfig;
import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.bookingservice.saga.SettlementSaga;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ItineraryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventConsumer.class);

    private final SettlementSaga        settlementSaga;
    private final SettlementRepository  settlementRepository;
    private final BookingRepository     bookingRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public ItineraryEventConsumer(SettlementSaga settlementSaga,
                                  SettlementRepository settlementRepository,
                                  BookingRepository bookingRepository,
                                  PaymentEventPublisher paymentEventPublisher) {
        this.settlementSaga        = settlementSaga;
        this.settlementRepository  = settlementRepository;
        this.bookingRepository     = bookingRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    /**
     * Handles ItineraryCompletedEvent — delegates to SettlementSaga which
     * creates SETTLEMENT_PENDING and publishes payment.initiated.
     * Idempotency guaranteed by unique constraint on settlements.itinerary_id.
     */
    @RabbitListener(queues = AmqpConfig.BOOKING_CONSUMER_QUEUE)
    public void onItineraryCompleted(ItineraryCompletedEvent event) {
        log.info("Received itinerary.completed: itineraryId={} userId={} amount={}",
                event.itineraryId(), event.userId(), event.totalAmount());
        settlementSaga.createPending(
                event.itineraryId(),
                event.userId(),
                event.totalAmount());
    }

    /**
     * Handles ItineraryCancelledEvent — cancels all CONFIRMED/PENDING bookings
     * and publishes payment.refunded if a settlement exists.
     */
    @RabbitListener(queues = "itinerary.events.booking-cancelled-consumer")
    @Transactional
    public void onItineraryCancelled(ItineraryCancelledEvent event) {
        log.info("Received itinerary.cancelled: itineraryId={} reason={}",
                event.itineraryId(), event.reason());

        List<Booking> bookings = bookingRepository.findByItineraryId(event.itineraryId());
        for (Booking booking : bookings) {
            if (booking.getStatus() == Booking.BookingStatus.CONFIRMED
                    || booking.getStatus() == Booking.BookingStatus.PENDING) {
                booking.setStatus(Booking.BookingStatus.CANCELLED);
            }
        }
        if (!bookings.isEmpty()) {
            bookingRepository.saveAll(bookings);
            log.info("Cancelled {} booking(s) for itineraryId={}", bookings.size(), event.itineraryId());
        }

        settlementRepository.findByItineraryId(event.itineraryId()).ifPresent(settlement -> {
            if (settlement.getStatus() == Settlement.SettlementStatus.SETTLEMENT_PENDING
                    || settlement.getStatus() == Settlement.SettlementStatus.SETTLED) {
                settlement.setStatus(Settlement.SettlementStatus.REFUNDED);
                settlementRepository.save(settlement);
                paymentEventPublisher.publishRefunded(
                        settlement.getId(), event.itineraryId(), settlement.getAmount());
                log.info("Settlement id={} set to REFUNDED for itineraryId={}",
                        settlement.getId(), event.itineraryId());
            }
        });
    }
}