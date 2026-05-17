package com.team15.tripplanning.bookingservice.feature.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.contracts.dto.BookingAggregateRequest;
import com.team15.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.team15.tripplanning.contracts.dto.ItineraryAggregateDTO;
import com.team15.tripplanning.contracts.dto.UserBookingTotalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingAggregateService — S5 aggregate endpoints")
class BookingAggregateServiceTest {

    @Mock BookingRepository bookingRepository;
    @InjectMocks BookingAggregateService service;

    // ── GET /user/{userId}/total ─────────────────────────────────────────────

    @Test
    @DisplayName("getUserTotal: returns correct totalAmount and tripCount")
    void getUserTotal_returnsCorrectValues() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 12, 31);

        when(bookingRepository.sumConfirmedAmountByUserAndRange(eq(1L), any(), any()))
                .thenReturn(2400.0);
        when(bookingRepository.countConfirmedByUserAndRange(eq(1L), any(), any()))
                .thenReturn(4L);

        UserBookingTotalDTO result = service.getUserTotal(1L, start, end);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2400));
        assertThat(result.tripCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("getUserTotal: 400 when startDate is after endDate")
    void getUserTotal_invalidDateRange_throws400() {
        assertThatThrownBy(() ->
                service.getUserTotal(1L, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("getUserTotal: null DB sum treated as zero")
    void getUserTotal_nullSumDefaultsToZero() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 12, 31);

        when(bookingRepository.sumConfirmedAmountByUserAndRange(eq(1L), any(), any()))
                .thenReturn(null);
        when(bookingRepository.countConfirmedByUserAndRange(eq(1L), any(), any()))
                .thenReturn(0L);

        UserBookingTotalDTO result = service.getUserTotal(1L, start, end);

        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.tripCount()).isEqualTo(0L);
    }

    // ── GET /itinerary/{itineraryId}/confirmed-summary ───────────────────────

    @Test
    @DisplayName("getConfirmedSummary: returns count and totalRevenue from DB")
    void getConfirmedSummary_returnsCorrectDTO() {
        when(bookingRepository.getConfirmedSummaryByItinerary(20L))
                .thenReturn(new Object[]{2L, 2000.0});

        ConfirmedSummaryDTO result = service.getConfirmedSummary(20L);

        assertThat(result.count()).isEqualTo(2L);
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    @DisplayName("getConfirmedSummary: count=0 and revenue=0 when no CONFIRMED bookings")
    void getConfirmedSummary_noBookings_returnsZeros() {
        when(bookingRepository.getConfirmedSummaryByItinerary(20L))
                .thenReturn(new Object[]{0L, 0.0});

        ConfirmedSummaryDTO result = service.getConfirmedSummary(20L);

        assertThat(result.count()).isEqualTo(0L);
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getConfirmedSummary: gracefully handles null/short array from DB")
    void getConfirmedSummary_nullArray_returnsZeros() {
        when(bookingRepository.getConfirmedSummaryByItinerary(20L))
                .thenReturn(null);

        ConfirmedSummaryDTO result = service.getConfirmedSummary(20L);

        assertThat(result.count()).isEqualTo(0L);
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── POST /aggregate-by-itineraries ───────────────────────────────────────

    @Test
    @DisplayName("aggregateByItineraries: returns correct totalBookings and totalRevenue")
    void aggregateByItineraries_returnsCorrectTotals() {
        BookingAggregateRequest req = new BookingAggregateRequest(
                List.of(1L, 2L), "2026-01-01", "2026-12-31", "CONFIRMED");

        when(bookingRepository.aggregateByItineraryIds(
                any(), any(), any(), eq(Booking.BookingStatus.CONFIRMED)))
                .thenReturn(new Object[]{3L, 3000.0});

        ItineraryAggregateDTO result = service.aggregateByItineraries(req);

        assertThat(result.totalBookings()).isEqualTo(3L);
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    @DisplayName("aggregateByItineraries: empty itineraryIds returns zero result without DB call")
    void aggregateByItineraries_emptyIds_returnsZeroImmediately() {
        BookingAggregateRequest req = new BookingAggregateRequest(
                List.of(), null, null, null);

        ItineraryAggregateDTO result = service.aggregateByItineraries(req);

        assertThat(result.totalBookings()).isEqualTo(0L);
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("aggregateByItineraries: null status treated as 'any status'")
    void aggregateByItineraries_nullStatus_treatedAsAny() {
        BookingAggregateRequest req = new BookingAggregateRequest(
                List.of(1L), "2026-01-01", "2026-12-31", null);

        when(bookingRepository.aggregateByItineraryIds(
                any(), any(), any(), eq(null)))
                .thenReturn(new Object[]{5L, 5000.0});

        ItineraryAggregateDTO result = service.aggregateByItineraries(req);

        assertThat(result.totalBookings()).isEqualTo(5L);
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("aggregateByItineraries: 400 when startDate is after endDate")
    void aggregateByItineraries_badDateRange_throws400() {
        BookingAggregateRequest req = new BookingAggregateRequest(
                List.of(1L), "2026-12-31", "2026-01-01", null);

        assertThatThrownBy(() -> service.aggregateByItineraries(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}