package com.team15.tripplanning.bookingservice.messaging.consumer;

import com.team15.tripplanning.bookingservice.messaging.publisher.AmqpConfig;
import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.model.Settlement.SettlementStatus;
import com.team15.tripplanning.bookingservice.model.mongo.PaymentAuditEvent;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.PaymentAuditEventRepository;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RabbitListener(queues = AmqpConfig.SAGA_QUEUE)
public class ItineraryEventConsumer {

    private final SettlementRepository settlementRepository;
    private final BookingRepository bookingRepository;
    private final PaymentAuditEventRepository paymentAuditEventRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public ItineraryEventConsumer(SettlementRepository settlementRepository,
                                 BookingRepository bookingRepository,
                                 PaymentAuditEventRepository paymentAuditEventRepository,
                                 PaymentEventPublisher paymentEventPublisher) {
        this.settlementRepository = settlementRepository;
        this.bookingRepository = bookingRepository;
        this.paymentAuditEventRepository = paymentAuditEventRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @RabbitHandler
    @Transactional
    public void onItineraryCompleted(ItineraryCompletedEvent event) {
        BigDecimal totalAmount = toBigDecimal(bookingRepository.sumConfirmedAmountByItineraryId(event.itineraryId()));
        Settlement settlement = new Settlement();
        settlement.setItineraryId(event.itineraryId());
        settlement.setUserId(event.userId());
        settlement.setAmount(totalAmount);
        settlement.setStatus(SettlementStatus.PENDING);

        try {
            settlement = settlementRepository.save(settlement);
        } catch (DataIntegrityViolationException ex) {
            return; // idempotent: settlement already exists
        }

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "SETTLEMENT_PENDING");
        auditDetails.put("itineraryId", event.itineraryId());
        auditDetails.put("settlementId", settlement.getId());
        auditDetails.put("userId", event.userId());
        auditDetails.put("amount", totalAmount.doubleValue());
        paymentAuditEventRepository.save(new PaymentAuditEvent(auditDetails));

        paymentEventPublisher.publishPaymentInitiated(
                new PaymentInitiatedEvent(settlement.getId(), event.itineraryId(), totalAmount)
        );
    }

    @RabbitHandler
    @Transactional
    public void onItineraryCancelled(ItineraryCancelledEvent event) {
        Long settlementId = settlementRepository.findByItineraryId(event.itineraryId())
                .map(Settlement::getId)
                .orElse(null);

        List<Booking> bookings = bookingRepository.findByItineraryId(event.itineraryId());
        for (Booking booking : bookings) {
            if (booking.getStatus() == Booking.BookingStatus.PENDING) {
                bookingRepository.updateStatusByIdAndStatus(
                        booking.getId(),
                        Booking.BookingStatus.CANCELLED.name(),
                        Booking.BookingStatus.PENDING.name()
                );
                continue;
            }

            if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
                int updated = bookingRepository.updateStatusByIdAndStatus(
                        booking.getId(),
                        Booking.BookingStatus.CANCELLED.name(),
                        Booking.BookingStatus.CONFIRMED.name()
                );
                if (updated == 1) {
                    BigDecimal refundAmount = toBigDecimal(booking.getAmount());
                    Map<String, Object> auditDetails = new HashMap<>();
                    auditDetails.put("action", "REFUNDED");
                    auditDetails.put("bookingId", booking.getId());
                    auditDetails.put("userId", booking.getUserId());
                    auditDetails.put("amount", refundAmount.doubleValue());
                    paymentAuditEventRepository.save(new PaymentAuditEvent(auditDetails));

                    paymentEventPublisher.publishPaymentRefunded(
                            new PaymentRefundedEvent(settlementId, booking.getItineraryId(), refundAmount)
                    );
                }
            }
        }

        settlementRepository.findByItineraryId(event.itineraryId()).ifPresent(settlement -> {
            if (settlement.getStatus() != SettlementStatus.REFUNDED) {
                settlement.setStatus(SettlementStatus.REFUNDED);
                settlement.setSettledAt(LocalDateTime.now());
                settlementRepository.save(settlement);
            }
        });
    }

    private BigDecimal toBigDecimal(Double value) {
        return BigDecimal.valueOf(value != null ? value : 0.0);
    }
}
