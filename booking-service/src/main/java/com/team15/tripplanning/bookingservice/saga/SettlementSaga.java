package com.team15.tripplanning.bookingservice.saga;

import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.model.Settlement.SettlementStatus;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
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
 *   createPending()   SETTLEMENT_PENDING  ← ItineraryCompletedEvent consumed
 *   process()         → SETTLED           ← POST /api/bookings/settlement/process (success)
 *                       publishes payment.completed
 *   fail()            → PAYMENT_FAILED    ← POST /api/bookings/settlement/process (failure)
 *                       publishes payment.failed
 *   refund()          → REFUNDED          ← ItineraryCancelledEvent consumed
 *                       publishes payment.refunded
 *
 * All mutations are transactional. The unique constraint on settlements.itinerary_id
 * is the idempotency guard — duplicate ItineraryCompletedEvents are silently ignored.
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

    // ── Step 1: Create SETTLEMENT_PENDING ─────────────────────────────────────

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
        log.info("SETTLEMENT_PENDING created: id={} itineraryId={} amount={}",
                s.getId(), itineraryId, amount);

        publisher.publishPaymentInitiated(s.getId(), itineraryId, amount);
        return s;
    }

    // ── Step 2a: Process → SETTLED ────────────────────────────────────────────

    /**
     * Called by POST /api/bookings/settlement/process.
     * Validates that amount > 0 and userId matches the settlement owner.
     * On success: SETTLED + publishes payment.completed.
     * On failure: PAYMENT_FAILED + publishes payment.failed.
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
            publisher.publishPaymentCompleted(s.getId(), itineraryId, s.getAmount());
        } else {
            String reason = amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                    ? "Invalid amount: " + amount
                    : "userId mismatch: expected=" + s.getUserId() + " got=" + userId;
            s.setStatus(SettlementStatus.FAILED);
            s.setFailureReason(reason);
            s = settlementRepository.save(s);
            log.warn("Settlement FAILED: id={} itineraryId={} reason={}",
                    s.getId(), itineraryId, reason);
            publisher.publishPaymentFailed(s.getId(), itineraryId, reason);
        }

        return s;
    }

    // ── Step 2b: Refund → REFUNDED ────────────────────────────────────────────

    /**
     * Called when itinerary.cancelled is consumed (compensation flow).
     * Transitions SETTLEMENT_PENDING or PAYMENT_FAILED → REFUNDED.
     */
    @Transactional
    public void refund(Long itineraryId) {
        settlementRepository.findByItineraryId(itineraryId).ifPresent(s -> {
            s.setStatus(SettlementStatus.REFUNDED);
            settlementRepository.save(s);
            log.info("Settlement REFUNDED: id={} itineraryId={}", s.getId(), itineraryId);
            publisher.publishPaymentRefunded(s.getId(), itineraryId, s.getAmount());
        });
    }
}
