package com.team15.tripplanning.destinationservice;

import com.team15.tripplanning.contracts.dto.DestinationBookingRevenueAggregateDTO;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import com.team15.tripplanning.destinationservice.dto.DestinationRevenueDTO;
import com.team15.tripplanning.destinationservice.messaging.publisher.DestinationEventPublisher;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.repository.DestinationReviewRepository;
import com.team15.tripplanning.destinationservice.service.DestinationSearchService;
import com.team15.tripplanning.destinationservice.service.DestinationService;
import com.team15.tripplanning.destinationservice.service.MongoEventLogger;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S2-F3: Get Destination Booking Revenue Summary
 * Endpoint: GET /api/destinations/{id}/revenue?startDate={date}&endDate={date}
 *
 * M3 change: native 3-table SQL JOIN replaced by a single Feign call to
 * itinerary-service which fans out to booking-service internally.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S2F3RevenueTest {

    @Mock DestinationRepository       destinationRepository;
    @Mock DestinationReviewRepository reviewRepository;
    @Mock MongoEventLogger            mongoEventLogger;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock DestinationSearchService    searchService;
    @Mock ItineraryServiceClient      itineraryServiceClient;
    @Mock UserServiceClient           userServiceClient;
    @Mock DestinationEventPublisher   eventPublisher;

    DestinationService service;

    private static final Long   DEST_ID    = 1L;
    private static final LocalDate START   = LocalDate.of(2026, 3, 1);
    private static final LocalDate END     = LocalDate.of(2026, 3, 31);

    @BeforeEach
    void setUp() {
        service = new DestinationService(
                destinationRepository, reviewRepository, mongoEventLogger,
                redisTemplate, searchService, itineraryServiceClient,
                userServiceClient, eventPublisher);

        // Suppress Redis wildcard deletes (soft dependency in tests)
        when(redisTemplate.keys(anyString())).thenReturn(null);

        Destination dest = new Destination();
        dest.setId(DEST_ID);
        dest.setName("Dahab");
        dest.setCountry("Egypt");
        dest.setCategory(DestinationCategory.ADVENTURE);
        dest.setStatus(DestinationStatus.ACTIVE);
        dest.setRating(0.0);
        dest.setTotalRatings(0);
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));
    }

    // ── Spec scenario 1 ──────────────────────────────────────────────────────

    /**
     * (setup) Destination ID=1 ("Dahab", ADVENTURE). 5 CONFIRMED bookings via
     * itinerary-service: amounts 200,300,400,500,600 in March 2026.
     * (action) GET /api/destinations/1/revenue?startDate=2026-03-01&endDate=2026-03-31
     * (expect) 200 — totalBookings=5, totalRevenue=2000.00, averageBookingAmount=400.00
     */
    @Test
    void revenueAggregateReturnedFromFeignChain() {
        when(itineraryServiceClient.getDestinationBookingRevenue(
                eq(DEST_ID), eq(START.toString()), eq(END.toString())))
                .thenReturn(new DestinationBookingRevenueAggregateDTO(
                        5L, new BigDecimal("2000.00"), new BigDecimal("400.00")));

        DestinationRevenueDTO result = service.getDestinationRevenueSummary(DEST_ID, START, END);

        assertThat(result.getTotalBookings()).isEqualTo(5L);
        assertThat(result.getTotalRevenue()).isEqualTo(2000.00);
        assertThat(result.getAverageBookingAmount()).isEqualTo(400.00);
        assertThat(result.getDestinationId()).isEqualTo(DEST_ID);
        assertThat(result.getName()).isEqualTo("Dahab");

        // Verify exactly one Feign call — no direct DB call to itinerary/booking tables
        verify(itineraryServiceClient)
                .getDestinationBookingRevenue(DEST_ID, START.toString(), END.toString());
    }

    // ── Spec scenario 2: destination not found ────────────────────────────────

    @Test
    void returns404WhenDestinationNotFound() {
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDestinationRevenueSummary(999L, START, END))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Spec scenario 3: itinerary-service unavailable ────────────────────────

    @Test
    void returns503WhenItineraryServiceUnavailable() {
        when(itineraryServiceClient.getDestinationBookingRevenue(any(), any(), any()))
                .thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> service.getDestinationRevenueSummary(DEST_ID, START, END))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // ── Spec scenario 4: invalid date range ───────────────────────────────────

    @Test
    void returns400WhenStartDateAfterEndDate() {
        assertThatThrownBy(() ->
                service.getDestinationRevenueSummary(DEST_ID, END, START)) // reversed
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
