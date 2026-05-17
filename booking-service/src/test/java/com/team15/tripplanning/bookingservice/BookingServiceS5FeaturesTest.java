package com.team15.tripplanning.bookingservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.team15.tripplanning.bookingservice.dto.CreateBookingRequest;
import com.team15.tripplanning.bookingservice.dto.DestinationSeasonRevenueDTO;
import com.team15.tripplanning.bookingservice.dto.RefundCancellationRequest;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;
import com.team15.tripplanning.bookingservice.messaging.publisher.PaymentEventPublisher;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingCouponRepository;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.CouponRepository;
import com.team15.tripplanning.bookingservice.repository.PaymentAuditEventRepository;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.bookingservice.service.BookingService;
import com.team15.tripplanning.bookingservice.service.MongoEventLogger;
import com.team15.tripplanning.contracts.dto.BatchDestinationRequest;
import com.team15.tripplanning.contracts.dto.BatchItineraryRequest;
import com.team15.tripplanning.contracts.dto.DestinationSummaryDTO;
import com.team15.tripplanning.contracts.dto.ItineraryDTO;
import com.team15.tripplanning.contracts.dto.ItinerarySummaryDTO;
import com.team15.tripplanning.contracts.dto.UserDTO;
import com.team15.tripplanning.contracts.events.PaymentRefundedEvent;
import com.team15.tripplanning.contracts.feign.DestinationServiceClient;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BookingService — S5-F3, S5-F4, S5-F10, S5-F12 feature tests")
class BookingServiceS5FeaturesTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingCouponRepository bookingCouponRepository;
    @Mock CouponRepository couponRepository;
    @Mock PaymentAuditEventRepository paymentAuditEventRepository;
    @Mock MongoEventLogger mongoEventLogger;
    @SuppressWarnings("unchecked")
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock UserServiceClient userServiceClient;
    @Mock ItineraryServiceClient itineraryServiceClient;
    @Mock DestinationServiceClient destinationServiceClient;
    @Mock PaymentEventPublisher paymentEventPublisher;
    @Mock SettlementRepository settlementRepository;

    private BookingService service() {
        return new BookingService(
                bookingRepository, bookingCouponRepository, couponRepository,
                paymentAuditEventRepository, mongoEventLogger, redisTemplate,
                userServiceClient, itineraryServiceClient, destinationServiceClient,
                paymentEventPublisher, settlementRepository);
    }

    // ─────────────────────────────── S5-F3 ──────────────────────────────────

    @Nested
    @DisplayName("S5-F3: getUserBookingSummary — Feign user-service existence check")
    class S5F3 {

        @Test
        @DisplayName("returns correct summary for existing user")
        void existingUser_returnsSummary() {
            UserDTO user = new UserDTO(1L, "Ahmed", "ahmed@test.com", null, "USER", "ACTIVE", null, null);
            when(userServiceClient.getUser(1L)).thenReturn(user);

            when(bookingRepository.findByUserId(1L)).thenReturn(List.of(
                    confirmedBooking(1L, 750.0, Booking.BookingType.ACCOMMODATION),
                    confirmedBooking(2L, 1200.0, Booking.BookingType.ACCOMMODATION),
                    confirmedBooking(3L, 300.0, Booking.BookingType.TRANSPORT),
                    confirmedBooking(4L, 150.0, Booking.BookingType.ACTIVITY)));

            when(bookingRepository.getBookingSummaryByUser(1L)).thenReturn(List.of(
                    new Object[]{"ACCOMMODATION", 1950.0},
                    new Object[]{"TRANSPORT", 300.0},
                    new Object[]{"ACTIVITY", 150.0}));

            UserBookingSummaryDTO result = service().getUserBookingSummary(1L);

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getTotalBookings()).isEqualTo(4);
            assertThat(result.getTotalAmount()).isEqualTo(2400.0);
            assertThat(result.getTypeBreakdown()).containsEntry("ACCOMMODATION", 1950.0)
                    .containsEntry("TRANSPORT", 300.0)
                    .containsEntry("ACTIVITY", 150.0);
        }

        @Test
        @DisplayName("404 when Feign throws FeignException.NotFound — no JDBC on users table")
        void unknownUser_throws404() {
            when(userServiceClient.getUser(999L))
                    .thenThrow(mock(FeignException.NotFound.class));

            assertThatThrownBy(() -> service().getUserBookingSummary(999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));

            // never queries bookings for non-existent user
            verify(bookingRepository, never()).findByUserId(anyLong());
        }
    }

    // ─────────────────────────────── S5-F4 ──────────────────────────────────

    @Nested
    @DisplayName("S5-F4: createFromRequest — Feign itinerary-service validation")
    class S5F4 {

        @Test
        @DisplayName("201 — creates booking with seasonal surcharge for PLANNED itinerary")
        void plannedItinerary_createsBooking() {
            ItineraryDTO itinerary = new ItineraryDTO(1L, 1L, 10L, "PLANNED", null, null, null, null);
            when(itineraryServiceClient.getItinerary(1L)).thenReturn(itinerary);
            when(itineraryServiceClient.getDestinationActiveCount(10L)).thenReturn(2); // off-peak

            Booking saved = new Booking();
            saved.setId(100L);
            saved.setItineraryId(1L);
            saved.setUserId(1L);
            saved.setAmount(1000.0);
            saved.setType(Booking.BookingType.ACCOMMODATION);
            saved.setStatus(Booking.BookingStatus.PENDING);
            saved.setBookingDetails(Map.of("seasonalSurcharge", 0.0));
            when(bookingRepository.save(any())).thenReturn(saved);
            when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreateBookingRequest req = new CreateBookingRequest();
            req.setItineraryId(1L);
            req.setAmount(1000.0);
            req.setType("ACCOMMODATION");
            req.setProviderName("Dahab Bay Hostel");
            req.setUserId(1L);

            Booking result = service().createFromRequest(req);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);

            // Feign calls made — no JDBC to itineraries table
            verify(itineraryServiceClient).getItinerary(1L);
            verify(itineraryServiceClient).getDestinationActiveCount(10L);
        }

        @Test
        @DisplayName("404 when Feign throws NotFound for unknown itinerary")
        void unknownItinerary_throws404() {
            when(itineraryServiceClient.getItinerary(999L))
                    .thenThrow(mock(FeignException.NotFound.class));

            CreateBookingRequest req = new CreateBookingRequest();
            req.setItineraryId(999L);
            req.setAmount(1000.0);
            req.setType("ACCOMMODATION");

            assertThatThrownBy(() -> service().createFromRequest(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("400 when itinerary status is DRAFT — booking not allowed")
        void draftItinerary_throws400() {
            ItineraryDTO itinerary = new ItineraryDTO(1L, 1L, 10L, "DRAFT", null, null, null, null);
            when(itineraryServiceClient.getItinerary(1L)).thenReturn(itinerary);

            CreateBookingRequest req = new CreateBookingRequest();
            req.setItineraryId(1L);
            req.setAmount(1000.0);
            req.setType("ACCOMMODATION");

            assertThatThrownBy(() -> service().createFromRequest(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("400 when itinerary status is COMPLETED — booking not allowed")
        void completedItinerary_throws400() {
            ItineraryDTO itinerary = new ItineraryDTO(1L, 1L, 10L, "COMPLETED", null, null, null, null);
            when(itineraryServiceClient.getItinerary(1L)).thenReturn(itinerary);

            CreateBookingRequest req = new CreateBookingRequest();
            req.setItineraryId(1L);
            req.setAmount(1000.0);
            req.setType("TRANSPORT");

            assertThatThrownBy(() -> service().createFromRequest(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("seasonal surcharge 20% applied when activeCount >= 5 (peak season)")
        void peakSeason_applies20PercentSurcharge() {
            ItineraryDTO itinerary = new ItineraryDTO(1L, 1L, 10L, "IN_PROGRESS", null, null, null, null);
            when(itineraryServiceClient.getItinerary(1L)).thenReturn(itinerary);
            when(itineraryServiceClient.getDestinationActiveCount(10L)).thenReturn(5); // peak

            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            when(bookingRepository.save(captor.capture())).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                b.setId(1L);
                return b;
            });
            when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreateBookingRequest req = new CreateBookingRequest();
            req.setItineraryId(1L);
            req.setAmount(1000.0);
            req.setType("ACCOMMODATION");
            req.setUserId(1L);

            service().createFromRequest(req);

            Booking saved = captor.getValue();
            assertThat(saved.getBookingDetails()).containsKey("seasonalSurcharge");
            double surcharge = ((Number) saved.getBookingDetails().get("seasonalSurcharge")).doubleValue();
            assertThat(surcharge).isEqualTo(200.0); // 20% of 1000
        }
    }

    // ─────────────────────────────── S5-F10 ─────────────────────────────────

    @Nested
    @DisplayName("S5-F10: getRevenueByDestinationAndSeason — batch Feign calls")
    class S5F10 {

        @Test
        @DisplayName("exactly two Feign batch calls made, results grouped by destination")
        void twoFeignCalls_groupedCorrectly() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end   = LocalDate.of(2026, 3, 31);

            Booking b1 = confirmedBookingWithSurcharge(1L, 10L, 1000.0, 200.0); // peak, itinerary→D1
            Booking b2 = confirmedBookingWithSurcharge(2L, 10L, 500.0, 100.0);  // peak, itinerary→D1
            Booking b3 = confirmedBookingWithSurcharge(3L, 11L, 800.0, 0.0);    // off-peak, itinerary→D2

            when(bookingRepository.findConfirmedByCreatedAtBetween(any(), any()))
                    .thenReturn(List.of(b1, b2, b3));

            when(itineraryServiceClient.batchGetItineraries(any(BatchItineraryRequest.class)))
                    .thenReturn(List.of(
                            new ItinerarySummaryDTO(10L, 1L, 1L, "COMPLETED"),
                            new ItinerarySummaryDTO(11L, 2L, 1L, "COMPLETED")));

            when(destinationServiceClient.batchGetDestinations(any(BatchDestinationRequest.class)))
                    .thenReturn(List.of(
                            new DestinationSummaryDTO(1L, "Dahab", "Egypt", "BEACH"),
                            new DestinationSummaryDTO(2L, "Marsa Alam", "Egypt", "BEACH")));

            when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<DestinationSeasonRevenueDTO> results =
                    service().getRevenueByDestinationAndSeason(start, end);

            // Exactly one batch call per downstream service
            verify(itineraryServiceClient, times(1)).batchGetItineraries(any());
            verify(destinationServiceClient, times(1)).batchGetDestinations(any());

            assertThat(results).hasSize(2);

            DestinationSeasonRevenueDTO dahab = results.stream()
                    .filter(r -> r.getDestinationName().equals("Dahab")).findFirst().orElseThrow();
            assertThat(dahab.getTotalRevenue()).isEqualTo(1500.0);
            assertThat(dahab.getSurchargeRevenue()).isEqualTo(300.0);
            assertThat(dahab.getBaseRevenue()).isEqualTo(1200.0);
            assertThat(dahab.getPeakBookingCount()).isEqualTo(2L);
            assertThat(dahab.getOffPeakBookingCount()).isEqualTo(0L);

            DestinationSeasonRevenueDTO marsa = results.stream()
                    .filter(r -> r.getDestinationName().equals("Marsa Alam")).findFirst().orElseThrow();
            assertThat(marsa.getTotalRevenue()).isEqualTo(800.0);
            assertThat(marsa.getSurchargeRevenue()).isEqualTo(0.0);
            assertThat(marsa.getPeakBookingCount()).isEqualTo(0L);
            assertThat(marsa.getOffPeakBookingCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("returns empty list when no CONFIRMED bookings in date range")
        void noBookings_returnsEmpty() {
            when(bookingRepository.findConfirmedByCreatedAtBetween(any(), any()))
                    .thenReturn(List.of());

            List<DestinationSeasonRevenueDTO> results =
                    service().getRevenueByDestinationAndSeason(
                            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

            assertThat(results).isEmpty();
            verifyNoInteractions(itineraryServiceClient, destinationServiceClient);
        }
    }

    // ─────────────────────────────── S5-F12 ─────────────────────────────────

    @Nested
    @DisplayName("S5-F12: processRefundCancellationTier — Feign + atomic UPDATE")
    class S5F12 {

        @Test
        @DisplayName("EarlyCancellation: 200 with full refund when trip is 30+ days away")
        void earlyCancel_fullRefund() {
            Booking booking = confirmedBooking(100L, 2000.0, Booking.BookingType.ACCOMMODATION);
            booking.setItineraryId(10L);
            when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

            ItineraryDTO itinerary = new ItineraryDTO(
                    10L, 1L, 5L, "PLANNED",
                    LocalDate.now().plusDays(30), null, null, null);
            when(itineraryServiceClient.getItinerary(10L)).thenReturn(itinerary);

            when(bookingRepository.updateStatusByIdAndStatus(100L, "CANCELLED", "CONFIRMED"))
                    .thenReturn(1);
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(settlementRepository.findByItineraryId(10L)).thenReturn(Optional.empty());

            RefundCancellationRequest req = new RefundCancellationRequest();
            req.setReason("schedule_conflict");

            Map<String, Object> result = service().processRefundCancellationTier(100L, req);

            assertThat(result.get("strategy")).isEqualTo("EarlyCancellationRefundStrategy");
            assertThat(result.get("tier")).isEqualTo("EARLY");
            assertThat(result.get("status")).isEqualTo("CANCELLED");

            // M3: atomic update used (not direct setStatus)
            verify(bookingRepository).updateStatusByIdAndStatus(100L, "CANCELLED", "CONFIRMED");
            // M3: payment.refunded published
            verify(paymentEventPublisher).publishPaymentRefunded(any(PaymentRefundedEvent.class));
        }

        @Test
        @DisplayName("NoRefundStrategy: 400 when itinerary is IN_PROGRESS (trip started)")
        void inProgressTrip_noRefund400() {
            Booking booking = confirmedBooking(101L, 2000.0, Booking.BookingType.ACCOMMODATION);
            booking.setItineraryId(10L);
            when(bookingRepository.findById(101L)).thenReturn(Optional.of(booking));

            ItineraryDTO itinerary = new ItineraryDTO(
                    10L, 1L, 5L, "IN_PROGRESS",
                    LocalDate.now().minusDays(1), null, null, null);
            when(itineraryServiceClient.getItinerary(10L)).thenReturn(itinerary);
            when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefundCancellationRequest req = new RefundCancellationRequest();
            req.setReason("changed_mind");

            assertThatThrownBy(() -> service().processRefundCancellationTier(101L, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));

            // REFUND_DENIED audit written; booking NOT cancelled
            verify(bookingRepository, never()).updateStatusByIdAndStatus(anyLong(), eq("CANCELLED"), eq("CONFIRMED"));
        }

        @Test
        @DisplayName("404 when Feign throws NotFound for itinerary — no JDBC to itinerary table")
        void missingItinerary_throws404() {
            Booking booking = confirmedBooking(102L, 2000.0, Booking.BookingType.ACCOMMODATION);
            booking.setItineraryId(999L);
            when(bookingRepository.findById(102L)).thenReturn(Optional.of(booking));
            when(itineraryServiceClient.getItinerary(999L))
                    .thenThrow(mock(FeignException.NotFound.class));

            assertThatThrownBy(() -> service().processRefundCancellationTier(102L, new RefundCancellationRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("404 when booking itself not found")
        void missingBooking_throws404() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().processRefundCancellationTier(999L, new RefundCancellationRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("409 Conflict when concurrent refund call wins the atomic UPDATE race")
        void concurrentRefund_throws409() {
            Booking booking = confirmedBooking(100L, 2000.0, Booking.BookingType.ACCOMMODATION);
            booking.setItineraryId(10L);
            when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

            ItineraryDTO itinerary = new ItineraryDTO(
                    10L, 1L, 5L, "PLANNED",
                    LocalDate.now().plusDays(30), null, null, null);
            when(itineraryServiceClient.getItinerary(10L)).thenReturn(itinerary);

            // Concurrent caller already moved the row out of CONFIRMED → rowcount 0
            when(bookingRepository.updateStatusByIdAndStatus(100L, "CANCELLED", "CONFIRMED"))
                    .thenReturn(0);

            RefundCancellationRequest req = new RefundCancellationRequest();
            req.setReason("schedule_conflict");

            assertThatThrownBy(() -> service().processRefundCancellationTier(100L, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("payment.refunded published carrying itineraryId after successful refund")
        void successRefund_publishesPaymentRefundedEvent() {
            Booking booking = confirmedBooking(100L, 2000.0, Booking.BookingType.ACCOMMODATION);
            booking.setItineraryId(10L);
            when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

            ItineraryDTO itinerary = new ItineraryDTO(
                    10L, 1L, 5L, "PLANNED",
                    LocalDate.now().plusDays(30), null, null, null);
            when(itineraryServiceClient.getItinerary(10L)).thenReturn(itinerary);
            when(bookingRepository.updateStatusByIdAndStatus(100L, "CANCELLED", "CONFIRMED")).thenReturn(1);
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentAuditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(settlementRepository.findByItineraryId(10L)).thenReturn(Optional.empty());

            service().processRefundCancellationTier(100L, new RefundCancellationRequest());

            ArgumentCaptor<PaymentRefundedEvent> captor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
            verify(paymentEventPublisher).publishPaymentRefunded(captor.capture());
            assertThat(captor.getValue().itineraryId()).isEqualTo(10L);
        }
    }

    // ─────────────────────────── helpers ────────────────────────────────────

    private Booking confirmedBooking(Long id, double amount, Booking.BookingType type) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId(1L);
        b.setAmount(amount);
        b.setType(type);
        b.setStatus(Booking.BookingStatus.CONFIRMED);
        b.setCreatedAt(LocalDateTime.now());
        return b;
    }

    private Booking confirmedBookingWithSurcharge(Long id, Long itineraryId,
                                                   double amount, double surcharge) {
        Booking b = confirmedBooking(id, amount, Booking.BookingType.ACCOMMODATION);
        b.setItineraryId(itineraryId);
        b.setBookingDetails(Map.of("seasonalSurcharge", surcharge));
        return b;
    }
}