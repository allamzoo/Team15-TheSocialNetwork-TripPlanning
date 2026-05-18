package com.team15.tripplanning.destinationservice;

import com.team15.tripplanning.contracts.dto.DestinationDashboardAggregateDTO;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.destinationservice.dto.DestinationDashboardDTO;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.service.DestinationDashboardService;
import com.team15.tripplanning.destinationservice.service.MongoEventLogger;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S2-F12: Get Destination Analytics Dashboard
 * Endpoint: GET /api/destinations/{id}/dashboard
 *
 * M3 change: totalItineraries / completedItineraries / totalVisitors fetched
 * via Feign to itinerary-service. M2 Builder, DASHBOARD_VIEWED MongoDB log,
 * and 10-min cache TTL are preserved.
 */
@ExtendWith(MockitoExtension.class)
class S2F12DashboardTest {

    @Mock DestinationRepository  destinationRepository;
    @Mock ItineraryServiceClient itineraryServiceClient;
    @Mock MongoEventLogger       mongoEventLogger;

    DestinationDashboardService service;

    private static final Long DEST_ID = 5L;

    @BeforeEach
    void setUp() {
        service = new DestinationDashboardService(
                destinationRepository, itineraryServiceClient, mongoEventLogger);
    }

    private Destination luxor(double rating, int totalRatings) {
        Destination d = new Destination();
        d.setId(DEST_ID);
        d.setName("Luxor");
        d.setCountry("Egypt");
        d.setCategory(DestinationCategory.HISTORICAL);
        d.setStatus(DestinationStatus.ACTIVE);
        d.setRating(rating);
        d.setTotalRatings(totalRatings);
        return d;
    }

    // ── Spec scenario 1 ──────────────────────────────────────────────────────

    /**
     * (setup) Destination ID=5 ("Luxor", rating=4.9, totalRatings=2).
     *         In itinerary-postgres: 5 itineraries — 3 PAID (3 distinct users),
     *         1 PLANNED, 1 CANCELLED.
     * (action) GET /api/destinations/5/dashboard with valid Bearer token.
     * (expect) 200 — totalItineraries=5, completedItineraries=3, totalVisitors=3,
     *                totalRatings=2, averageRating=4.9.
     */
    @Test
    void dashboardAggregateBuiltFromFeignAndLocalRatingData() {
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(luxor(4.9, 2)));
        when(itineraryServiceClient.getDestinationDashboardAggregate(DEST_ID))
                .thenReturn(new DestinationDashboardAggregateDTO(5L, 3L, 3L));

        DestinationDashboardDTO result = service.getDashboard(DEST_ID);

        assertThat(result.getTotalItineraries()).isEqualTo(5L);
        assertThat(result.getCompletedItineraries()).isEqualTo(3L);
        assertThat(result.getTotalVisitors()).isEqualTo(3L);
        assertThat(result.getTotalRatings()).isEqualTo(2);
        assertThat(result.getAverageRating()).isEqualTo(4.9);
        assertThat(result.getCompletionRate()).isEqualTo(60.0); // 3/5 * 100

        // M2 behaviour: DASHBOARD_VIEWED always logged (even on cache hit)
        verify(mongoEventLogger).onEvent(eq("DASHBOARD_VIEWED"), any());
        // Feign call was made once
        verify(itineraryServiceClient).getDestinationDashboardAggregate(DEST_ID);
    }

    // ── Spec scenario 2: destination not found ────────────────────────────────

    /**
     * GET /api/destinations/999/dashboard → 404.
     */
    @Test
    void returns404WhenDestinationDoesNotExist() {
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDashboard(999L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Spec scenario 3: itinerary-service unavailable — dashboard still returns ─

    /**
     * Feign throws → dashboard falls back to zeros for itinerary fields.
     * Local rating data (totalRatings, averageRating) is unaffected.
     */
    @Test
    void dashboardFallsBackToZerosWhenItineraryServiceUnavailable() {
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(luxor(4.9, 2)));
        when(itineraryServiceClient.getDestinationDashboardAggregate(DEST_ID))
                .thenThrow(mock(FeignException.class));

        DestinationDashboardDTO result = service.getDashboard(DEST_ID);

        // Itinerary fields fall back to zero
        assertThat(result.getTotalItineraries()).isEqualTo(0L);
        assertThat(result.getCompletedItineraries()).isEqualTo(0L);
        assertThat(result.getTotalVisitors()).isEqualTo(0L);

        // Local rating data must still be present
        assertThat(result.getTotalRatings()).isEqualTo(2);
        assertThat(result.getAverageRating()).isEqualTo(4.9);

        // MongoDB log still fires despite Feign failure
        verify(mongoEventLogger).onEvent(eq("DASHBOARD_VIEWED"), any());
    }

    // ── Spec scenario 4: completionRate computed correctly ────────────────────

    @Test
    void completionRateIsZeroWhenNoItineraries() {
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(luxor(0.0, 0)));
        when(itineraryServiceClient.getDestinationDashboardAggregate(DEST_ID))
                .thenReturn(new DestinationDashboardAggregateDTO(0L, 0L, 0L));

        DestinationDashboardDTO result = service.getDashboard(DEST_ID);

        assertThat(result.getCompletionRate()).isEqualTo(0.0);
    }
}
