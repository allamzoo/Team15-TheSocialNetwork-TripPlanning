package com.team15.tripplanning.bookingservice.saga;

import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.model.Settlement.SettlementStatus;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import com.team15.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * S5-EVENTS — Settlement saga state machine.
 *
 * State transitions:
 *
 *   createPending()   PENDING     ← ItineraryCompletedEvent consumed
 *   process()         → COMPLETED ← POST /api/bookings/settlement/process (success)
 *                       publishes payment.completed
 *   fail()            → FAILED    ← POST /api/bookings/settlement/process (failure)
 *                       publishes payment.failed
 *   refund()          → REFUNDED  ← ItineraryCancelledEvent consumed
 *                       publishes payment.refunded
 *
 * NOTE: The active saga handling is done directly by ItineraryEventConsumer and
 * feature.settlement.SettlementService. This class is retained for reference.
 */
@Service
public class SettlementSaga {

    private static final Logger log = LoggerFactory.getLogger(SettlementSaga.class);

    private final SettlementRepository    settlementRepository;
    private final PaymentEventPublisher   publisher;

    public SettlementSaga(SettlementRepository settlementRepository,
                          PaymentEventPublisher publisher) {
        this.settlementRepository = settlementRepository;
        this.publisher            = publisher;
    }

    // ── Step 1: Create PENDING ────────────────────────────────────────────────

    /**
     * Called by ItineraryEventConsumer on ItineraryCompletedEvent.
     * Idempotent — if a row for this itinerary already exists, logs and returns it.
     */
    @Transactional
    public Settlement createPending(Long itineraryId, Long userId, BigDecimal amount) {
        if (settlementRepository.existsByItineraryId(itineraryId)) {
            log.warn("Settlement already exists for itineraryId={} — skipping duplicate event",
                    itineraryId);
            return settlementRepository.findByItineraryId(itineraryId).orElseThrow();
        }

        Settlement s = new Settlement(itineraryId, userId, amount);
        s = settlementRepository.save(s);
        log.info("PENDING settlement created: id={} itineraryId={} amount={}",
                s.getId(), itineraryId, amount);

        publisher.publishPaymentInitiated(new PaymentInitiatedEvent(s.getId(), itineraryId, amount));
        return s;
    }

    // ── Step 2a: Process → COMPLETED ─────────────────────────────────────────

    /**
     * Called by POST /api/bookings/settlement/process.
     * Validates that amount > 0 and userId matches the settlement owner.
     * On success: COMPLETED + publishes payment.completed.
     * On failure: FAILED + publishes payment.failed.
     */
    @Transactional
    public Settlement process(Long itineraryId, Long userId, BigDecimal amount) {
        Settlement s = settlementRepository.findByItineraryId(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No settlement found for itineraryId=" + itineraryId));

        if (s.getStatus() != SettlementStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Settlement is already in state " + s.getStatus());
        }

        // Validation: amount must be > 0 and userId must match
        boolean valid = amount != null
                && amount.compareTo(BigDecimal.ZERO) > 0
                && userId != null
                && userId.equals(s.getUserId());

        if (valid) {
            s.setStatus(SettlementStatus.COMPLETED);
            s.setSettledAt(LocalDateTime.now());
            s = settlementRepository.save(s);
            log.info("Settlement COMPLETED: id={} itineraryId={}", s.getId(), itineraryId);
            publisher.publishPaymentCompleted(new PaymentCompletedEvent(s.getId(), itineraryId, s.getAmount()));
        } else {
            String reason = amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                    ? "Invalid amount: " + amount
                    : "userId mismatch: expected=" + s.getUserId() + " got=" + userId;
            s.setStatus(SettlementStatus.FAILED);
            s.setFailureReason(reason);
            s = settlementRepository.save(s);
            log.warn("Settlement FAILED: id={} itineraryId={} reason={}",
                    s.getId(), itineraryId, reason);
            publisher.publishPaymentFailed(new PaymentFailedEvent(s.getId(), itineraryId, reason));
        }

        return s;
    }

    // ── Step 2b: Refund → REFUNDED ────────────────────────────────────────────

    /**
     * Called when itinerary.cancelled is consumed (compensation flow).
     * Transitions PENDING or FAILED → REFUNDED.
     */
    @Transactional
    public void refund(Long itineraryId) {
        settlementRepository.findByItineraryId(itineraryId).ifPresent(s -> {
            s.setStatus(SettlementStatus.REFUNDED);
            settlementRepository.save(s);
            log.info("Settlement REFUNDED: id={} itineraryId={}", s.getId(), itineraryId);
            publisher.publishPaymentRefunded(new PaymentRefundedEvent(s.getId(), itineraryId, s.getAmount()));
        });
    }
}
