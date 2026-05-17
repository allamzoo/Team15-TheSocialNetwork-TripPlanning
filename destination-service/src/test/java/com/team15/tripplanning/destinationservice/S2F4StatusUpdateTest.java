package com.team15.tripplanning.destinationservice;

import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S2-F4: Update Destination Status
 * Endpoint: PUT /api/destinations/{id}/status
 *
 * M3 change: INACTIVE guard replaced — no longer queries itineraries table
 * directly. Uses Feign → itinerary-service /active-count instead.
 * After a successful change: destination.status-changed published.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S2F4StatusUpdateTest {

    @Mock DestinationRepository       destinationRepository;
    @Mock DestinationReviewRepository reviewRepository;
    @Mock MongoEventLogger            mongoEventLogger;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock DestinationSearchService    searchService;
    @Mock ItineraryServiceClient      itineraryServiceClient;
    @Mock UserServiceClient           userServiceClient;
    @Mock DestinationEventPublisher   eventPublisher;

    DestinationService service;

    private static final Long DEST_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new DestinationService(
                destinationRepository, reviewRepository, mongoEventLogger,
                redisTemplate, searchService, itineraryServiceClient,
                userServiceClient, eventPublisher);
        when(redisTemplate.keys(anyString())).thenReturn(null);
    }

    private Destination activeDestination() {
        Destination d = new Destination();
        d.setId(DEST_ID);
        d.setName("Dahab");
        d.setCountry("Egypt");
        d.setCategory(DestinationCategory.ADVENTURE);
        d.setStatus(DestinationStatus.ACTIVE);
        d.setRating(0.0);
        d.setTotalRatings(0);
        return d;
    }

    // ── Spec scenario 1 ──────────────────────────────────────────────────────

    /**
     * (setup) Destination ID=1, status=ACTIVE. Itinerary: destinationId=1, status=PLANNED.
     * (action) PUT body {"status":"INACTIVE"} → Feign returns active-count=1.
     * (expect) 400 — cannot mark INACTIVE with active itineraries.
     */
    @Test
    void blocksInactiveTransitionWhenActiveItinerariesExist() {
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(activeDestination()));
        when(itineraryServiceClient.getDestinationActiveCount(DEST_ID)).thenReturn(1);

        assertThatThrownBy(() -> service.updateDestinationStatus(DEST_ID, "INACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(itineraryServiceClient).getDestinationActiveCount(DEST_ID);
    }

    // ── Spec scenario 2 ──────────────────────────────────────────────────────

    /**
     * (action) Feign returns active-count=0.
     * (expect) 200 — destination status=INACTIVE. destination.status-changed published.
     */
    @Test
    void allowsInactiveTransitionWhenNoActiveItineraries() {
        Destination dest = activeDestination();
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryServiceClient.getDestinationActiveCount(DEST_ID)).thenReturn(0);

        Destination result = service.updateDestinationStatus(DEST_ID, "INACTIVE");

        assertThat(result.getStatus()).isEqualTo(DestinationStatus.INACTIVE);
        verify(eventPublisher).publishStatusChanged(DEST_ID, "ACTIVE", "INACTIVE");
    }

    // ── Spec scenario 3 ──────────────────────────────────────────────────────

    /**
     * (action) PUT body {"status":"ACTIVE"} → no Feign call needed → 200, event published.
     */
    @Test
    void allowsActiveTransitionWithoutFeignCall() {
        Destination dest = activeDestination();
        dest.setStatus(DestinationStatus.INACTIVE);
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Destination result = service.updateDestinationStatus(DEST_ID, "ACTIVE");

        assertThat(result.getStatus()).isEqualTo(DestinationStatus.ACTIVE);
        // No active-count Feign call for non-INACTIVE transition
        verify(itineraryServiceClient, never()).getDestinationActiveCount(any());
        verify(eventPublisher).publishStatusChanged(DEST_ID, "INACTIVE", "ACTIVE");
    }

    // ── Spec scenario 4 ──────────────────────────────────────────────────────

    @Test
    void allowsSeasonalTransitionWithoutFeignCall() {
        Destination dest = activeDestination();
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));
        when(destinationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Destination result = service.updateDestinationStatus(DEST_ID, "SEASONAL");

        assertThat(result.getStatus()).isEqualTo(DestinationStatus.SEASONAL);
        verify(itineraryServiceClient, never()).getDestinationActiveCount(any());
        verify(eventPublisher).publishStatusChanged(DEST_ID, "ACTIVE", "SEASONAL");
    }

    // ── Spec scenario 5: itinerary-service down ───────────────────────────────

    @Test
    void returns503WhenItineraryServiceUnavailableForActiveCountCheck() {
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(activeDestination()));
        when(itineraryServiceClient.getDestinationActiveCount(DEST_ID))
                .thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> service.updateDestinationStatus(DEST_ID, "INACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // ── Spec scenario 6: invalid status value ─────────────────────────────────

    @Test
    void returns400ForUnrecognisedStatus() {
        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(activeDestination()));

        assertThatThrownBy(() -> service.updateDestinationStatus(DEST_ID, "UNKNOWN"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Spec scenario 7: destination not found ────────────────────────────────

    @Test
    void returns404WhenDestinationNotFound() {
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDestinationStatus(999L, "INACTIVE"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
