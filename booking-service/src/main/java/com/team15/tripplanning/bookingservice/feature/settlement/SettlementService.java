package com.team15.tripplanning.bookingservice.feature.settlement;

import com.team15.tripplanning.bookingservice.dto.SettlementProcessRequest;
import com.team15.tripplanning.bookingservice.dto.SettlementResultDTO;
import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.model.mongo.PaymentAuditEvent;
import com.team15.tripplanning.bookingservice.repository.PaymentAuditEventRepository;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.bookingservice.security.JwtService;
import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final PaymentAuditEventRepository paymentAuditEventRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final JwtService jwtService;

    public SettlementService(SettlementRepository settlementRepository,
                             PaymentAuditEventRepository paymentAuditEventRepository,
                             PaymentEventPublisher paymentEventPublisher,
                             JwtService jwtService) {
        this.settlementRepository = settlementRepository;
        this.paymentAuditEventRepository = paymentAuditEventRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.jwtService = jwtService;
    }

    public SettlementDTO getByItineraryId(Long itineraryId) {
        return settlementRepository.findByItineraryId(itineraryId)
                .map(SettlementDTO::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No settlement for itineraryId=" + itineraryId));
    }

    @Transactional
    public SettlementResultDTO processSettlement(SettlementProcessRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getItineraryId() == null || request.getUserId() == null || request.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itineraryId, userId, and amount are required");
        }

        Settlement settlement = settlementRepository.findByItineraryIdForUpdate(request.getItineraryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Saga has not yet completed for this itinerary; cannot settle"));

        if (settlement.getStatus() == Settlement.SettlementStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already in flight");
        }
        if (settlement.getStatus() == Settlement.SettlementStatus.COMPLETED) {
            return SettlementResultDTO.from(
                    settlement.getId(),
                    settlement.getItineraryId(),
                    settlement.getUserId(),
                    settlement.getAmount(),
                    settlement.getStatus().name(),
                    settlement.getSettledAt(),
                    settlement.getFailureReason()
            );
        }
        if (settlement.getStatus() == Settlement.SettlementStatus.FAILED
                || settlement.getStatus() == Settlement.SettlementStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already finalized");
        }

        Long jwtUserId = extractUserId(httpRequest);
        if (jwtUserId == null || !jwtUserId.equals(request.getUserId()) || !jwtUserId.equals(settlement.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this settlement");
        }

        if (settlement.getAmount() == null || settlement.getAmount().compareTo(request.getAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount mismatch");
        }

        int moved = settlementRepository.updateStatusIfMatches(
                settlement.getId(),
                Settlement.SettlementStatus.PROCESSING,
                Settlement.SettlementStatus.PENDING
        );
        if (moved == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement already in flight");
        }

        boolean approved = request.getAmount().compareTo(BigDecimal.ZERO) > 0;
        if (approved) {
            settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
            settlement.setSettledAt(LocalDateTime.now());
            settlementRepository.save(settlement);

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("action", "SETTLEMENT_COMPLETED");
            auditDetails.put("settlementId", settlement.getId());
            auditDetails.put("itineraryId", settlement.getItineraryId());
            auditDetails.put("userId", settlement.getUserId());
            auditDetails.put("amount", settlement.getAmount().doubleValue());
            paymentAuditEventRepository.save(new PaymentAuditEvent(auditDetails));

            paymentEventPublisher.publishPaymentCompleted(
                    new PaymentCompletedEvent(settlement.getId(), settlement.getItineraryId(), settlement.getAmount())
            );

            return SettlementResultDTO.from(
                    settlement.getId(),
                    settlement.getItineraryId(),
                    settlement.getUserId(),
                    settlement.getAmount(),
                    settlement.getStatus().name(),
                    settlement.getSettledAt(),
                    settlement.getFailureReason()
            );
        }

        settlement.setStatus(Settlement.SettlementStatus.FAILED);
        settlement.setSettledAt(LocalDateTime.now());
        settlement.setFailureReason("Payment rejected");
        settlementRepository.save(settlement);

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "SETTLEMENT_FAILED");
        auditDetails.put("settlementId", settlement.getId());
        auditDetails.put("itineraryId", settlement.getItineraryId());
        auditDetails.put("userId", settlement.getUserId());
        auditDetails.put("amount", settlement.getAmount().doubleValue());
        auditDetails.put("reason", settlement.getFailureReason());
        paymentAuditEventRepository.save(new PaymentAuditEvent(auditDetails));

        paymentEventPublisher.publishPaymentFailed(
                new PaymentFailedEvent(settlement.getId(), settlement.getItineraryId(), settlement.getFailureReason())
        );

        return SettlementResultDTO.from(
                settlement.getId(),
                settlement.getItineraryId(),
                settlement.getUserId(),
                settlement.getAmount(),
                settlement.getStatus().name(),
                settlement.getSettledAt(),
                settlement.getFailureReason()
        );
    }

    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7);
        return jwtService.extractUserId(token);
    }
}