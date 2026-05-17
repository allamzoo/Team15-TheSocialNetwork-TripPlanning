package com.team15.tripplanning.bookingservice.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.PaymentAuditEventRepository;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentInitiatedEvent;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItineraryEventConsumer — S5-EVENTS saga consumer")
class ItineraryEventConsumerTest {

    @Mock SettlementRepository settlementRepository;
    @Mock BookingRepository bookingRepository;
    @Mock PaymentAuditEventRepository paymentAuditEventRepository;
    @Mock PaymentEventPublisher paymentEventPublisher;

    @InjectMocks ItineraryEventConsumer consumer;

    // ─────────────────────────── itinerary.completed ────────────────────────

    @Test
    @DisplayName("completed: creates PENDING settlement and publishes payment.initiated")
    void onCompleted_createsSettlementAndPublishes() {
        ItineraryCompletedEvent event =
                new ItineraryCompletedEvent(10L, 1L, 5L, BigDecimal.valueOf(2000));
        when(bookingRepository.sumConfirmedAmountByItineraryId(10L)).thenReturn(2000.0);

        Settlement saved = new Settlement();
        saved.setId(42L);
        saved.setItineraryId(10L);
        saved.setUserId(1L);
        saved.setAmount(BigDecimal.valueOf(2000));
        saved.setStatus(Settlement.SettlementStatus.PENDING);
        when(settlementRepository.save(any())).thenReturn(saved);
        when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onItineraryCompleted(event);

        // settlement saved with PENDING status and correct fields
        ArgumentCaptor<Settlement> sCaptor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(sCaptor.capture());
        assertThat(sCaptor.getValue().getStatus()).isEqualTo(Settlement.SettlementStatus.PENDING);
        assertThat(sCaptor.getValue().getItineraryId()).isEqualTo(10L);
        assertThat(sCaptor.getValue().getUserId()).isEqualTo(1L);

        // audit row written for SETTLEMENT_PENDING
        verify(paymentAuditEventRepository).save(any());

        // payment.initiated published with correct settlementId, itineraryId, amount
        ArgumentCaptor<PaymentInitiatedEvent> eCaptor = ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(paymentEventPublisher).publishPaymentInitiated(eCaptor.capture());
        assertThat(eCaptor.getValue().settlementId()).isEqualTo(42L);
        assertThat(eCaptor.getValue().itineraryId()).isEqualTo(10L);
        assertThat(eCaptor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    @DisplayName("completed: DataIntegrityViolation swallowed — idempotent, no publish")
    void onCompleted_idempotentOnDuplicate() {
        ItineraryCompletedEvent event =
                new ItineraryCompletedEvent(10L, 1L, 5L, BigDecimal.valueOf(2000));
        when(bookingRepository.sumConfirmedAmountByItineraryId(10L)).thenReturn(2000.0);
        when(settlementRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup key"));

        consumer.onItineraryCompleted(event);

        // duplicate delivery → no further side effects
        verifyNoInteractions(paymentEventPublisher);
        verifyNoInteractions(paymentAuditEventRepository);
    }

    @Test
    @DisplayName("completed: null sum from DB treated as zero amount")
    void onCompleted_nullSumDefaultsToZero() {
        ItineraryCompletedEvent event =
                new ItineraryCompletedEvent(99L, 2L, 5L, BigDecimal.ZERO);
        when(bookingRepository.sumConfirmedAmountByItineraryId(99L)).thenReturn(null);

        Settlement saved = new Settlement();
        saved.setId(7L);
        saved.setAmount(BigDecimal.ZERO);
        when(settlementRepository.save(any())).thenReturn(saved);
        when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onItineraryCompleted(event);

        ArgumentCaptor<PaymentInitiatedEvent> eCaptor = ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(paymentEventPublisher).publishPaymentInitiated(eCaptor.capture());
        assertThat(eCaptor.getValue().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("completed: settlement amount equals sum of CONFIRMED bookings, not event.totalAmount")
    void onCompleted_amountComesFromBookingsNotEvent() {
        // event carries 5000 but DB says 2000 — consumer should use DB value
        ItineraryCompletedEvent event =
                new ItineraryCompletedEvent(11L, 1L, 5L, BigDecimal.valueOf(5000));
        when(bookingRepository.sumConfirmedAmountByItineraryId(11L)).thenReturn(2000.0);

        Settlement saved = new Settlement();
        saved.setId(3L);
        saved.setAmount(BigDecimal.valueOf(2000));
        when(settlementRepository.save(any())).thenReturn(saved);
        when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onItineraryCompleted(event);

        ArgumentCaptor<Settlement> sCaptor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(sCaptor.capture());
        assertThat(sCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    // ─────────────────────────── itinerary.cancelled ────────────────────────

    @Test
    @DisplayName("cancelled: PENDING bookings cancelled, no refund event")
    void onCancelled_pendingCancelledNoRefundEvent() {
        ItineraryCancelledEvent event =
                new ItineraryCancelledEvent(20L, 1L, 5L, "user_requested");

        Booking pending = new Booking();
        pending.setId(100L);
        pending.setItineraryId(20L);
        pending.setUserId(1L);
        pending.setAmount(500.0);
        pending.setStatus(Booking.BookingStatus.PENDING);

        when(bookingRepository.findByItineraryId(20L)).thenReturn(List.of(pending));
        when(settlementRepository.findByItineraryId(20L)).thenReturn(Optional.empty());
        when(bookingRepository.updateStatusByIdAndStatus(100L, "CANCELLED", "PENDING")).thenReturn(1);

        consumer.onItineraryCancelled(event);

        verify(bookingRepository).updateStatusByIdAndStatus(100L, "CANCELLED", "PENDING");
        verifyNoInteractions(paymentEventPublisher);
    }

    @Test
    @DisplayName("cancelled: CONFIRMED booking → CANCELLED + REFUNDED audit + payment.refunded")
    void onCancelled_confirmedRefunded() {
        ItineraryCancelledEvent event =
                new ItineraryCancelledEvent(20L, 1L, 5L, "user_requested");

        Booking confirmed = new Booking();
        confirmed.setId(200L);
        confirmed.setItineraryId(20L);
        confirmed.setUserId(1L);
        confirmed.setAmount(1200.0);
        confirmed.setStatus(Booking.BookingStatus.CONFIRMED);

        Settlement settlement = new Settlement();
        settlement.setId(55L);
        settlement.setStatus(Settlement.SettlementStatus.PENDING);

        when(bookingRepository.findByItineraryId(20L)).thenReturn(List.of(confirmed));
        when(settlementRepository.findByItineraryId(20L)).thenReturn(Optional.of(settlement));
        when(bookingRepository.updateStatusByIdAndStatus(200L, "CANCELLED", "CONFIRMED")).thenReturn(1);
        when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(settlementRepository.save(any())).thenReturn(settlement);

        consumer.onItineraryCancelled(event);

        verify(bookingRepository).updateStatusByIdAndStatus(200L, "CANCELLED", "CONFIRMED");
        verify(paymentAuditEventRepository).save(any());

        ArgumentCaptor<PaymentRefundedEvent> eCaptor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(paymentEventPublisher).publishPaymentRefunded(eCaptor.capture());
        assertThat(eCaptor.getValue().settlementId()).isEqualTo(55L);
        assertThat(eCaptor.getValue().itineraryId()).isEqualTo(20L);
        assertThat(eCaptor.getValue().refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1200));
    }

    @Test
    @DisplayName("cancelled: rowcount=0 → idempotent, no refund published")
    void onCancelled_idempotentWhenAlreadyCancelled() {
        ItineraryCancelledEvent event =
                new ItineraryCancelledEvent(20L, 1L, 5L, "user_requested");

        Booking confirmed = new Booking();
        confirmed.setId(200L);
        confirmed.setItineraryId(20L);
        confirmed.setAmount(1200.0);
        confirmed.setStatus(Booking.BookingStatus.CONFIRMED);

        when(bookingRepository.findByItineraryId(20L)).thenReturn(List.of(confirmed));
        when(settlementRepository.findByItineraryId(20L)).thenReturn(Optional.empty());
        // concurrent caller already cancelled it → rowcount 0
        when(bookingRepository.updateStatusByIdAndStatus(200L, "CANCELLED", "CONFIRMED")).thenReturn(0);

        consumer.onItineraryCancelled(event);

        verifyNoInteractions(paymentEventPublisher);
        verifyNoInteractions(paymentAuditEventRepository);
    }

    @Test
    @DisplayName("cancelled: settlement transitioned to REFUNDED with settledAt set")
    void onCancelled_settlementSetToRefunded() {
        ItineraryCancelledEvent event =
                new ItineraryCancelledEvent(30L, 1L, 5L, "user_requested");
        when(bookingRepository.findByItineraryId(30L)).thenReturn(List.of());

        Settlement settlement = new Settlement();
        settlement.setId(99L);
        settlement.setStatus(Settlement.SettlementStatus.PENDING);
        when(settlementRepository.findByItineraryId(30L)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any())).thenReturn(settlement);

        consumer.onItineraryCancelled(event);

        ArgumentCaptor<Settlement> sCaptor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(sCaptor.capture());
        assertThat(sCaptor.getValue().getStatus()).isEqualTo(Settlement.SettlementStatus.REFUNDED);
        assertThat(sCaptor.getValue().getSettledAt()).isNotNull();
    }

    @Test
    @DisplayName("cancelled: already REFUNDED settlement not double-saved")
    void onCancelled_alreadyRefundedSettlementSkipped() {
        ItineraryCancelledEvent event =
                new ItineraryCancelledEvent(30L, 1L, 5L, "user_requested");
        when(bookingRepository.findByItineraryId(30L)).thenReturn(List.of());

        Settlement settlement = new Settlement();
        settlement.setId(99L);
        settlement.setStatus(Settlement.SettlementStatus.REFUNDED);
        when(settlementRepository.findByItineraryId(30L)).thenReturn(Optional.of(settlement));

        consumer.onItineraryCancelled(event);

        // already REFUNDED → save not called again
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelled: multiple CONFIRMED bookings → one payment.refunded per booking")
    void onCancelled_multipleConfirmedEachGetRefundEvent() {
        ItineraryCancelledEvent event =
                new ItineraryCancelledEvent(40L, 1L, 5L, "user_requested");

        Booking b1 = new Booking(); b1.setId(1L); b1.setItineraryId(40L); b1.setAmount(500.0); b1.setStatus(Booking.BookingStatus.CONFIRMED);
        Booking b2 = new Booking(); b2.setId(2L); b2.setItineraryId(40L); b2.setAmount(700.0); b2.setStatus(Booking.BookingStatus.CONFIRMED);

        when(bookingRepository.findByItineraryId(40L)).thenReturn(List.of(b1, b2));
        when(settlementRepository.findByItineraryId(40L)).thenReturn(Optional.empty());
        when(bookingRepository.updateStatusByIdAndStatus(1L, "CANCELLED", "CONFIRMED")).thenReturn(1);
        when(bookingRepository.updateStatusByIdAndStatus(2L, "CANCELLED", "CONFIRMED")).thenReturn(1);
        when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onItineraryCancelled(event);

        verify(paymentEventPublisher, times(2)).publishPaymentRefunded(any());
        verify(paymentAuditEventRepository, times(2)).save(any());
    }
}