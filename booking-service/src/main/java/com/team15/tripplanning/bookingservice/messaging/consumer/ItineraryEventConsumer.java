package com.team15.tripplanning.bookingservice.messaging.consumer;

import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.model.SettlementStatus;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "m3.messaging.enabled", havingValue = "true")
public class ItineraryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventConsumer.class);

    private final SettlementRepository settlementRepository;
    private final BookingRepository bookingRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public ItineraryEventConsumer(SettlementRepository settlementRepository,
                                   BookingRepository bookingRepository,
                                   PaymentEventPublisher paymentEventPublisher) {
        this.settlementRepository = settlementRepository;
        this.bookingRepository = bookingRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    /**
     * Handles ItineraryCompletedEvent from itinerary-service.
     * Creates a Settlement row (PENDING) and publishes PaymentInitiatedEvent.
     * Idempotent: unique index on settlements.itinerary_id guards duplicate delivery.
     */
    @RabbitListener(queues = "itinerary.events.booking-consumer")
    @Transactional
    public void onItineraryCompleted(ItineraryCompletedEvent event) {
        log.info("Received itinerary.completed — itineraryId={} userId={} amount={}",
                event.itineraryId(), event.userId(), event.totalAmount());

        // Idempotency guard: if a settlement already exists, skip
        if (settlementRepository.findByItineraryId(event.itineraryId()).isPresent()) {
            log.info("Settlement already exists for itineraryId={} — skipping (idempotent)", event.itineraryId());
            return;
        }

        Settlement settlement = new Settlement();
        settlement.setItineraryId(event.itineraryId());
        settlement.setUserId(event.userId());
        settlement.setAmount(event.totalAmount());
        settlement.setStatus(SettlementStatus.PENDING);

        try {
            Settlement saved = settlementRepository.save(settlement);
            log.info("Created Settlement id={} for itineraryId={} (PENDING)", saved.getId(), event.itineraryId());

            paymentEventPublisher.publishPaymentInitiated(
                    saved.getId(), event.itineraryId(), event.totalAmount());
        } catch (DataIntegrityViolationException ex) {
            // Race condition: another instance inserted just before us — safe to ignore
            log.warn("Duplicate settlement insert race for itineraryId={} — ignoring", event.itineraryId());
        }
    }

    /**
     * Handles ItineraryCancelledEvent from itinerary-service.
     * Cancels all CONFIRMED/PENDING bookings for the itinerary and publishes PaymentRefundedEvent.
     */
    @RabbitListener(queues = "itinerary.events.booking-cancelled-consumer")
    @Transactional
    public void onItineraryCancelled(ItineraryCancelledEvent event) {
        log.info("Received itinerary.cancelled — itineraryId={} reason={}",
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

        // Update settlement to REFUNDED if one exists
        settlementRepository.findByItineraryId(event.itineraryId()).ifPresent(settlement -> {
            if (settlement.getStatus() == SettlementStatus.PENDING
                    || settlement.getStatus() == SettlementStatus.PROCESSING
                    || settlement.getStatus() == SettlementStatus.COMPLETED) {
                settlement.setStatus(SettlementStatus.REFUNDED);
                settlementRepository.save(settlement);
                paymentEventPublisher.publishPaymentRefunded(
                        settlement.getId(), event.itineraryId(), settlement.getAmount());
                log.info("Settlement id={} set to REFUNDED for itineraryId={}", settlement.getId(), event.itineraryId());
            }
        });
    }
}
