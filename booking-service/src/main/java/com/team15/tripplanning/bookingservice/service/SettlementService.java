package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.dto.SettlementProcessRequest;
import com.team15.tripplanning.bookingservice.dto.SettlementResultDTO;
import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.model.SettlementStatus;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final SettlementRepository settlementRepository;
    private final MongoEventLogger mongoEventLogger;
    private final PaymentEventPublisher paymentEventPublisher;

    public SettlementService(SettlementRepository settlementRepository,
                             MongoEventLogger mongoEventLogger,
                             PaymentEventPublisher paymentEventPublisher) {
        this.settlementRepository = settlementRepository;
        this.mongoEventLogger = mongoEventLogger;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    /**
     * POST /api/bookings/settlement/process — full state-machine per §7 + §1.5.
     *
     * @param request   {itineraryId, userId, amount} from request body
     * @param jwtUserId uid claim extracted from the Bearer token by the controller
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SettlementResultDTO processSettlement(SettlementProcessRequest request, Long jwtUserId) {

        // Step 1 — SELECT … FOR UPDATE (row-level pessimistic lock)
        Settlement settlement = settlementRepository
                .findByItineraryIdForUpdate(request.getItineraryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saga has not yet completed for this itinerary; cannot settle"));

        // Step 2 — Status gate
        switch (settlement.getStatus()) {
            case PROCESSING ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already in flight");
            case COMPLETED -> {
                log.info("Settlement {} already COMPLETED — returning prior result (idempotent re-call)", settlement.getId());
                return toDTO(settlement);
            }
            case FAILED ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already failed — use cancellation flow");
            case REFUNDED ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already refunded — itinerary was cancelled");
            case PENDING -> { /* proceed */ }
        }

        // Step 3 — Authorization: JWT uid must equal body.userId AND settlement.userId
        if (!jwtUserId.equals(request.getUserId()) || !jwtUserId.equals(settlement.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "JWT user does not match settlement owner");
        }
        // Amount replay-protection
        if (request.getAmount() == null || request.getAmount().compareTo(settlement.getAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Request amount does not match settlement amount");
        }

        // Step 4 — Atomic transition PENDING → PROCESSING
        settlement.setStatus(Settlement.SettlementStatus.PROCESSING);
        settlementRepository.saveAndFlush(settlement); // flush so concurrent callers see PROCESSING
        log.info("Settlement {} transitioned PENDING → PROCESSING", settlement.getId());

        // Step 5 — Run settlement logic (Trip Planning: always succeeds when pre-checks pass)
        // Step 6a — Success path
        settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
        settlement.setSettledAt(LocalDateTime.now());
        settlementRepository.save(settlement);

        // Write SETTLEMENT_COMPLETED to MongoDB (observability)
        Map<String, Object> auditPayload = new HashMap<>();
        auditPayload.put("settlementId", settlement.getId());
        auditPayload.put("itineraryId", settlement.getItineraryId());
        auditPayload.put("userId", settlement.getUserId());
        auditPayload.put("amount", settlement.getAmount().toPlainString());
        mongoEventLogger.onEvent("SETTLEMENT_COMPLETED", auditPayload);

        // Publish payment.completed (no-op until S5-EVENTS wires RabbitMQ)
        paymentEventPublisher.publishPaymentCompleted(
                settlement.getId(), settlement.getItineraryId(), settlement.getAmount());

        log.info("Settlement {} COMPLETED — itineraryId={} amount={}",
                settlement.getId(), settlement.getItineraryId(), settlement.getAmount());

        return toDTO(settlement);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private SettlementResultDTO toDTO(Settlement s) {
        SettlementResultDTO dto = new SettlementResultDTO();
        dto.setSettlementId(s.getId());
        dto.setItineraryId(s.getItineraryId());
        dto.setUserId(s.getUserId());
        dto.setAmount(s.getAmount());
        dto.setStatus(s.getStatus().name());
        dto.setSettledAt(s.getSettledAt());
        dto.setFailureReason(s.getFailureReason());
        return dto;
    }
}
