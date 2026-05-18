package com.team15.tripplanning.destinationservice;

import com.team15.tripplanning.contracts.dto.UserDTO;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import com.team15.tripplanning.destinationservice.messaging.publisher.DestinationEventPublisher;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationReview;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S2-F8: Verify Destination Review
 * Endpoint: PUT /api/destinations/{destinationId}/reviews/{reviewId}/verify
 *
 * M3 change: ADMIN check via Feign to user-service (replaces direct SQL on users table).
 * Feign 404 → 403 (do not leak user existence).
 * visitDate validation happens BEFORE the Feign call.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S2F8VerifyReviewTest {

    @Mock DestinationRepository       destinationRepository;
    @Mock DestinationReviewRepository reviewRepository;
    @Mock MongoEventLogger            mongoEventLogger;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock DestinationSearchService    searchService;
    @Mock ItineraryServiceClient      itineraryServiceClient;
    @Mock UserServiceClient           userServiceClient;
    @Mock DestinationEventPublisher   eventPublisher;

    DestinationService service;

    private static final Long DEST_ID    = 1L;
    private static final Long REVIEW_ID  = 5L;
    private static final Long ADMIN_ID   = 3L;
    private static final Long TRAVELER_ID = 1L;
    private static final Long MISSING_ID = 999L;

    private Destination destination;
    private DestinationReview review;

    @BeforeEach
    void setUp() {
        service = new DestinationService(
                destinationRepository, reviewRepository, mongoEventLogger,
                redisTemplate, searchService, itineraryServiceClient,
                userServiceClient, eventPublisher);
        when(redisTemplate.keys(anyString())).thenReturn(null);

        destination = new Destination();
        destination.setId(DEST_ID);
        destination.setName("Dahab");
        destination.setCountry("Egypt");
        destination.setCategory(DestinationCategory.ADVENTURE);
        destination.setStatus(DestinationStatus.ACTIVE);
        destination.setRating(0.0);
        destination.setTotalRatings(0);
        destination.setDestinationReviews(new ArrayList<>());

        review = new DestinationReview();
        review.setId(REVIEW_ID);
        review.setDestination(destination);
        review.setVisitDate(LocalDate.of(2026, 1, 15)); // past date
        review.setRating(4);
        review.setVerified(false);
        review.setContent("Great place");

        when(destinationRepository.findById(DEST_ID)).thenReturn(Optional.of(destination));
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
    }

    private UserDTO user(Long id, String role) {
        return new UserDTO(id, "Test User", "test@example.com",
                "01012345678", role, "ACTIVE", null, LocalDateTime.now());
    }

    // ── Spec scenario 1 ──────────────────────────────────────────────────────

    /**
     * (setup) Destination ID=1, Review ID=5 (visitDate=2026-01-15, verified=false).
     *         User ID=3 role=ADMIN.
     * (action) PUT /api/destinations/1/reviews/5/verify body {verifiedBy:3}
     * (expect) 200 — review verified=true, metadata has verifiedAt + verifiedBy=3.
     */
    @Test
    void adminCanVerifyReview() {
        when(userServiceClient.getUser(ADMIN_ID)).thenReturn(user(ADMIN_ID, "ADMIN"));

        service.verifyReview(DEST_ID, REVIEW_ID, ADMIN_ID);

        assertThat(review.getVerified()).isTrue();
        assertThat(review.getMetadata()).containsKey("verifiedAt");
        assertThat(review.getMetadata()).containsEntry("verifiedBy", ADMIN_ID);
        verify(userServiceClient).getUser(ADMIN_ID);
    }

    // ── Spec scenario 2: non-admin user ──────────────────────────────────────

    /**
     * verifiedBy:1 (TRAVELER user) → Feign returns role=TRAVELER → 403.
     */
    @Test
    void returns403WhenVerifierIsNotAdmin() {
        when(userServiceClient.getUser(TRAVELER_ID)).thenReturn(user(TRAVELER_ID, "TRAVELER"));

        assertThatThrownBy(() -> service.verifyReview(DEST_ID, REVIEW_ID, TRAVELER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Spec scenario 3: non-existent user ───────────────────────────────────

    /**
     * verifiedBy:999 (non-existent) → Feign returns 404 → 403 (existence not leaked).
     */
    @Test
    void returns403WhenVerifierUserDoesNotExist() {
        when(userServiceClient.getUser(MISSING_ID))
                .thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> service.verifyReview(DEST_ID, REVIEW_ID, MISSING_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN); // 403, NOT 404 — existence is not leaked
    }

    // ── Spec scenario 4: future visitDate ────────────────────────────────────

    /**
     * Review with visitDate in the future → 400.
     * This is validated BEFORE the Feign call (no downstream hop wasted).
     */
    @Test
    void returns400WhenVisitDateIsInFuture() {
        review.setVisitDate(LocalDate.now().plusDays(10));

        // No Feign call should be made for a future visit date
        assertThatThrownBy(() -> service.verifyReview(DEST_ID, REVIEW_ID, ADMIN_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Verify user-service was NOT called (validation happens before Feign hop)
        org.mockito.Mockito.verifyNoInteractions(userServiceClient);
    }

    // ── Spec scenario 5: review belongs to different destination ─────────────

    /**
     * Review belonging to a different destination → 400.
     */
    @Test
    void returns400WhenReviewBelongsToDifferentDestination() {
        Destination otherDest = new Destination();
        otherDest.setId(2L); // different destination
        review.setDestination(otherDest);

        assertThatThrownBy(() -> service.verifyReview(DEST_ID, REVIEW_ID, ADMIN_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Spec scenario 6: user-service unavailable ────────────────────────────

    @Test
    void returns503WhenUserServiceUnavailable() {
        when(userServiceClient.getUser(ADMIN_ID))
                .thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> service.verifyReview(DEST_ID, REVIEW_ID, ADMIN_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
