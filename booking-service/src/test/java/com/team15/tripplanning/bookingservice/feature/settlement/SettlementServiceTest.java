package com.team15.tripplanning.bookingservice.feature.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.team15.tripplanning.bookingservice.dto.SettlementProcessRequest;
import com.team15.tripplanning.bookingservice.dto.SettlementResultDTO;
import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.model.Settlement.SettlementStatus;
import com.team15.tripplanning.bookingservice.repository.PaymentAuditEventRepository;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.bookingservice.security.JwtService;
import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SettlementService — full idempotency lifecycle (§1.5 + §7 spec)")
class SettlementServiceTest {

    @Mock SettlementRepository settlementRepository;
    @Mock PaymentAuditEventRepository paymentAuditEventRepository;
    @Mock PaymentEventPublisher paymentEventPublisher;
    @Mock JwtService jwtService;
    @Mock HttpServletRequest httpRequest;

    @InjectMocks SettlementService settlementService;

    @BeforeEach
    void stubJwt() {
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer tok");
        when(jwtService.extractUserId("tok")).thenReturn(1L);
    }

    private Settlement buildSettlement(SettlementStatus status) {
        Settlement s = new Settlement();
        s.setId(1L);
        s.setItineraryId(20L);
        s.setUserId(1L);
        s.setAmount(BigDecimal.valueOf(2000));
        s.setStatus(status);
        return s;
    }

    private SettlementProcessRequest buildRequest() {
        SettlementProcessRequest req = new SettlementProcessRequest();
        req.setItineraryId(20L);
        req.setUserId(1L);
        req.setAmount(BigDecimal.valueOf(2000));
        return req;
    }

    // ── Step 1: 404 when no settlement row ──────────────────────────────────

    @Test
    @DisplayName("(1) 404 — no settlement row exists for itinerary")
    void step1_notFound_throws404() {
        when(settlementRepository.findByItineraryIdForUpdate(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.processSettlement(buildRequest(), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── Step 2: inspect locked status ───────────────────────────────────────

    @Test
    @DisplayName("(2a) 409 — status is PROCESSING (already in flight)")
    void step2_processing_throws409() {
        when(settlementRepository.findByItineraryIdForUpdate(20L))
                .thenReturn(Optional.of(buildSettlement(SettlementStatus.PROCESSING)));

        assertThatThrownBy(() -> settlementService.processSettlement(buildRequest(), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("(2b) 200 — status is COMPLETED, returns prior result without re-publishing")
    void step2_completed_returns200PriorResult() {
        Settlement s = buildSettlement(SettlementStatus.COMPLETED);
        s.setSettledAt(LocalDateTime.now());
        when(settlementRepository.findByItineraryIdForUpdate(20L)).thenReturn(Optional.of(s));

        SettlementResultDTO result = settlementService.processSettlement(buildRequest(), httpRequest);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSettlementId()).isEqualTo(1L);
        verifyNoInteractions(paymentEventPublisher, paymentAuditEventRepository);
    }

    @Test
    @DisplayName("(2c) 409 — status is FAILED (terminal)")
    void step2_failed_throws409() {
        when(settlementRepository.findByItineraryIdForUpdate(20L))
                .thenReturn(Optional.of(buildSettlement(SettlementStatus.FAILED)));

        assertThatThrownBy(() -> settlementService.processSettlement(buildRequest(), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("(2c) 409 — status is REFUNDED (terminal)")
    void step2_refunded_throws409() {
        when(settlementRepository.findByItineraryIdForUpdate(20L))
                .thenReturn(Optional.of(buildSettlement(SettlementStatus.REFUNDED)));

        assertThatThrownBy(() -> settlementService.processSettlement(buildRequest(), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ── Step 3: authorization ────────────────────────────────────────────────

    @Test
    @DisplayName("(3) 403 — JWT uid does not match body.userId")
    void step3_jwtUserMismatch_throws403() {
        when(settlementRepository.findByItineraryIdForUpdate(20L))
                .thenReturn(Optional.of(buildSettlement(SettlementStatus.PENDING)));
        when(jwtService.extractUserId("tok")).thenReturn(999L); // different user

        assertThatThrownBy(() -> settlementService.processSettlement(buildRequest(), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("(3) 400 — request amount differs from settlement.amount")
    void step3_amountMismatch_throws400() {
        when(settlementRepository.findByItineraryIdForUpdate(20L))
                .thenReturn(Optional.of(buildSettlement(SettlementStatus.PENDING)));

        SettlementProcessRequest req = buildRequest();
        req.setAmount(BigDecimal.valueOf(9999)); // wrong

        assertThatThrownBy(() -> settlementService.processSettlement(req, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ── Step 4: atomic PENDING → PROCESSING ─────────────────────────────────

    @Test
    @DisplayName("(4) 409 — UPDATE rowcount=0, concurrent caller already won the race")
    void step4_concurrentRace_throws409() {
        when(settlementRepository.findByItineraryIdForUpdate(20L))
                .thenReturn(Optional.of(buildSettlement(SettlementStatus.PENDING)));
        when(settlementRepository.updateStatusIfMatches(
                1L, SettlementStatus.PROCESSING, SettlementStatus.PENDING)).thenReturn(0);

        assertThatThrownBy(() -> settlementService.processSettlement(buildRequest(), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ── Step 6a: success path PENDING → COMPLETED ───────────────────────────

    @Test
    @DisplayName("(6a) success: COMPLETED, audit row written, payment.completed published")
    void step6a_success_completedAndPublishes() {
        when(settlementRepository.findByItineraryIdForUpdate(20L))
                .thenReturn(Optional.of(buildSettlement(SettlementStatus.PENDING)));
        when(settlementRepository.updateStatusIfMatches(
                1L, SettlementStatus.PROCESSING, SettlementStatus.PENDING)).thenReturn(1);
        when(settlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SettlementResultDTO result = settlementService.processSettlement(buildRequest(), httpRequest);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSettlementId()).isEqualTo(1L);
        assertThat(result.getItineraryId()).isEqualTo(20L);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));

        verify(paymentAuditEventRepository).save(any());
        verify(paymentEventPublisher).publishPaymentCompleted(any(PaymentCompletedEvent.class));
        verify(paymentEventPublisher, never()).publishPaymentFailed(any());
    }

    @Test
    @DisplayName("(6a) success: published PaymentCompletedEvent carries correct settlementId + itineraryId")
    void step6a_completedEvent_hasCorrectFields() {
        when(settlementRepository.findByItineraryIdForUpdate(20L))
                .thenReturn(Optional.of(buildSettlement(SettlementStatus.PENDING)));
        when(settlementRepository.updateStatusIfMatches(
                1L, SettlementStatus.PROCESSING, SettlementStatus.PENDING)).thenReturn(1);
        when(settlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        settlementService.processSettlement(buildRequest(), httpRequest);

        var captor = org.mockito.ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisher).publishPaymentCompleted(captor.capture());
        assertThat(captor.getValue().settlementId()).isEqualTo(1L);
        assertThat(captor.getValue().itineraryId()).isEqualTo(20L);
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    // ── Step 6b: failure path PENDING → FAILED ───────────────────────────────

    @Test
    @DisplayName("(6b) failure: amount=0 → FAILED, payment.failed published")
    void step6b_zeroAmount_failedAndPublishes() {
        Settlement s = new Settlement();
        s.setId(1L); s.setItineraryId(20L); s.setUserId(1L);
        s.setAmount(BigDecimal.ZERO); s.setStatus(SettlementStatus.PENDING);

        when(settlementRepository.findByItineraryIdForUpdate(20L)).thenReturn(Optional.of(s));
        when(settlementRepository.updateStatusIfMatches(
                1L, SettlementStatus.PROCESSING, SettlementStatus.PENDING)).thenReturn(1);
        when(settlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SettlementProcessRequest req = new SettlementProcessRequest();
        req.setItineraryId(20L); req.setUserId(1L); req.setAmount(BigDecimal.ZERO);

        SettlementResultDTO result = settlementService.processSettlement(req, httpRequest);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(paymentEventPublisher).publishPaymentFailed(any(PaymentFailedEvent.class));
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any());
    }

    // ── 400: missing required fields ─────────────────────────────────────────

    @Test
    @DisplayName("400 when request body is null")
    void nullRequest_throws400() {
        assertThatThrownBy(() -> settlementService.processSettlement(null, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("400 when itineraryId is missing from request")
    void missingItineraryId_throws400() {
        SettlementProcessRequest req = new SettlementProcessRequest();
        req.setUserId(1L); req.setAmount(BigDecimal.valueOf(2000));

        assertThatThrownBy(() -> settlementService.processSettlement(req, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}