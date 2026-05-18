package com.team15.tripplanning.destinationservice;

import com.team15.tripplanning.contracts.dto.ItineraryDTO;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import com.team15.tripplanning.destinationservice.dto.DestinationRateRequest;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S2-F7: Rate a Destination After Visit
 * Endpoint: POST /api/destinations/{id}/rate
 *
 * M3 change: itinerary validation via Feign (replaces direct SQL on shared DB).
 * Accepts COMPLETED or PAID status (PAID is M3 saga terminal state).
 * Publishes destination.rated after a successful rating.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S2F7RateDestinationTest {

    @Mock DestinationRepository       destinationRepository;
    @Mock DestinationReviewRepository reviewRepository;
    @Mock MongoEventLogger            mongoEventLogger;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock DestinationSearchService    searchService;
    @Mock ItineraryServiceClient      itineraryServiceClient;
    @Mock UserServiceClient           userServiceClient;
    @Mock DestinationEventPublisher   eventPublisher;

    DestinationService service;

    private static final Long DEST_ID      = 1L;
    private static final Long ITINERARY_ID = 10L;
    private static final Long USER_ID      = 42L;

    @BeforeEach
    void setUp() {
        service = new DestinationService(
                destinationRepository, reviewRepository, mongoEventLogger,
                redisTemplate, searchService, itineraryServiceClient,
                userServiceClient, eventPublisher);
        when(redisTemplate.keys(anyString())).thenReturn(null);

        Destination dest = destination(0.0, 0);
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Destination destination(double rating, int totalRatings) {
        Destination d = new Destination();
        d.setId(DEST_ID);
        d.setName("Dahab");
        d.setCountry("Egypt");
        d.setCategory(DestinationCategory.ADVENTURE);
        d.setStatus(DestinationStatus.ACTIVE);
        d.setRating(rating);
        d.setTotalRatings(totalRatings);
        return d;
    }

    private ItineraryDTO completedItinerary(Long itineraryId, String status) {
        return new ItineraryDTO(itineraryId, USER_ID, DEST_ID, status,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10),
                new BigDecimal("1000"), null);
    }

    // ── Spec scenario 1 ──────────────────────────────────────────────────────

    /**
     * (setup) Destination ID=1 (rating=0, totalRatings=0). Itinerary 10: COMPLETED.
     * (action) POST /api/destinations/1/rate body {itineraryId:10, rating:5}
     * (expect) 200 — rating=5.0, totalRatings=1. destination.rated published.
     */
    @Test
    void firstRatingComputesAverageCorrectly() {
        when(itineraryServiceClient.getItinerary(ITINERARY_ID))
                .thenReturn(completedItinerary(ITINERARY_ID, "COMPLETED"));

        Destination result = service.rateDestination(DEST_ID, new DestinationRateRequest(ITINERARY_ID, 5));

        assertThat(result.getRating()).isEqualTo(5.0);
        assertThat(result.getTotalRatings()).isEqualTo(1);
        verify(eventPublisher).publishRated(DEST_ID, ITINERARY_ID, 5.0, USER_ID);
    }

    // ── Spec scenario 2 ──────────────────────────────────────────────────────

    /**
     * Rate again with a different completed itinerary (ID=11) and rating=3
     * → running average = (5+3)/2 = 4.0, totalRatings=2.
     */
    @Test
    void secondRatingUpdatesRunningAverage() {
        // Destination already has 1 rating of 5.0
        Destination dest = destination(5.0, 1);
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));

        Long itinerary11 = 11L;
        when(itineraryServiceClient.getItinerary(itinerary11))
                .thenReturn(new ItineraryDTO(itinerary11, USER_ID, DEST_ID, "COMPLETED",
                        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 7),
                        new BigDecimal("800"), null));

        Destination result = service.rateDestination(DEST_ID, new DestinationRateRequest(itinerary11, 3));

        assertThat(result.getRating()).isEqualTo(4.0);
        assertThat(result.getTotalRatings()).isEqualTo(2);
    }

    // ── Spec scenario 3: PAID status accepted (M3 backward compat) ───────────

    /**
     * Itinerary in PAID status (M3 saga terminal) must also be accepted.
     */
    @Test
    void acceptsPaidStatusAsValidForRating() {
        when(itineraryServiceClient.getItinerary(ITINERARY_ID))
                .thenReturn(completedItinerary(ITINERARY_ID, "PAID"));

        Destination result = service.rateDestination(DEST_ID, new DestinationRateRequest(ITINERARY_ID, 4));

        assertThat(result.getTotalRatings()).isEqualTo(1);
    }

    // ── Spec scenario 4: itinerary not found ─────────────────────────────────

    /**
     * POST body {itineraryId:99, rating:4} → Feign returns 404 → throw 404.
     */
    @Test
    void returns404WhenItineraryNotFound() {
        when(itineraryServiceClient.getItinerary(99L))
                .thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() ->
                service.rateDestination(DEST_ID, new DestinationRateRequest(99L, 4)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Spec scenario 5: wrong status ────────────────────────────────────────

    /**
     * Itinerary status=PLANNED → 400 (must be COMPLETED or PAID).
     */
    @Test
    void returns400WhenItineraryStatusIsPlanned() {
        when(itineraryServiceClient.getItinerary(ITINERARY_ID))
                .thenReturn(completedItinerary(ITINERARY_ID, "PLANNED"));

        assertThatThrownBy(() ->
                service.rateDestination(DEST_ID, new DestinationRateRequest(ITINERARY_ID, 4)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Spec scenario 6: rating out of range ─────────────────────────────────

    /**
     * POST body {itineraryId:10, rating:6} → 400 (rating must be 1–5).
     */
    @Test
    void returns400WhenRatingIsAbove5() {
        assertThatThrownBy(() ->
                service.rateDestination(DEST_ID, new DestinationRateRequest(ITINERARY_ID, 6)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void returns400WhenRatingIsBelow1() {
        assertThatThrownBy(() ->
                service.rateDestination(DEST_ID, new DestinationRateRequest(ITINERARY_ID, 0)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Spec scenario 7: itinerary references different destination ───────────

    /**
     * Itinerary references destinationId=2 but caller is rating destinationId=1 → 400.
     */
    @Test
    void returns400WhenItineraryReferencesWrongDestination() {
        ItineraryDTO wrongDest = new ItineraryDTO(ITINERARY_ID, USER_ID,
                2L, // different destination
                "COMPLETED",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10),
                new BigDecimal("1000"), null);
        when(itineraryServiceClient.getItinerary(ITINERARY_ID)).thenReturn(wrongDest);

        assertThatThrownBy(() ->
                service.rateDestination(DEST_ID, new DestinationRateRequest(ITINERARY_ID, 4)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
