package com.team15.tripplanning.contracts.feign.fallback;

import com.team15.tripplanning.contracts.dto.BatchItineraryRequest;
import com.team15.tripplanning.contracts.dto.DestinationBookingRevenueAggregateDTO;
import com.team15.tripplanning.contracts.dto.DestinationDashboardAggregateDTO;
import com.team15.tripplanning.contracts.dto.ItineraryDTO;
import com.team15.tripplanning.contracts.dto.ItinerarySummaryDTO;
import com.team15.tripplanning.contracts.dto.UserTripSummaryAggregateDTO;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Safe-default fallback for {@link ItineraryServiceClient}.
 *
 * <p>Use {@link #SAFE} directly in catch blocks:
 * <pre>
 *   try {
 *       return itineraryServiceClient.getUserItinerarySummary(userId);
 *   } catch (FeignException e) {
 *       log.warn("itinerary-service unavailable: {}", e.getMessage());
 *       return ItineraryServiceFallback.SAFE.getUserItinerarySummary(userId);
 *   }
 * </pre>
 *
 * <p>Alternatively, declare as a Spring {@code @Bean} in your service's
 * {@code feign.FeignClientConfig} and inject wherever needed.
 */
public final class ItineraryServiceFallback implements ItineraryServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ItineraryServiceFallback.class);

    /** Singleton for use without Spring context (catch blocks, tests). */
    public static final ItineraryServiceFallback SAFE = new ItineraryServiceFallback();

    // ── Display / aggregate endpoints — return empty/zero (caller keeps working) ──

    @Override
    public UserTripSummaryAggregateDTO getUserItinerarySummary(Long userId) {
        log.debug("itinerary-service fallback: getUserItinerarySummary({})", userId);
        return UserTripSummaryAggregateDTO.empty();
    }

    @Override
    public int getActiveItineraryCount(Long userId) {
        log.debug("itinerary-service fallback: getActiveItineraryCount({})", userId);
        return 0;
    }

    @Override
    public long getCompletedItineraryCount(Long userId) {
        log.debug("itinerary-service fallback: getCompletedItineraryCount({})", userId);
        return 0L;
    }

    @Override
    public DestinationBookingRevenueAggregateDTO getDestinationBookingRevenue(
            Long destinationId, String startDate, String endDate) {
        log.debug("itinerary-service fallback: getDestinationBookingRevenue({})", destinationId);
        return new DestinationBookingRevenueAggregateDTO(0L, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Override
    public int getDestinationActiveCount(Long destinationId) {
        log.debug("itinerary-service fallback: getDestinationActiveCount({})", destinationId);
        // Return 0 — conservatively: won't block INACTIVE status transition.
        // S2-F4 guard will allow the transition (count < 1 → no active itineraries).
        return 0;
    }

    @Override
    public DestinationDashboardAggregateDTO getDestinationDashboardAggregate(Long destinationId) {
        log.debug("itinerary-service fallback: getDestinationDashboardAggregate({})", destinationId);
        return new DestinationDashboardAggregateDTO(0L, 0L, 0L);
    }

    @Override
    public List<ItinerarySummaryDTO> batchGetItineraries(BatchItineraryRequest request) {
        log.debug("itinerary-service fallback: batchGetItineraries");
        return List.of();
    }

    // ── Validation / guard endpoints — return null so callers reject the request ──

    /**
     * Returns {@code null} when itinerary-service is unavailable.
     * <b>Callers must null-check</b> and respond with 503 / 404 as appropriate:
     * <pre>
     *   ItineraryDTO dto = ...;
     *   if (dto == null) throw new ResponseStatusException(SERVICE_UNAVAILABLE);
     * </pre>
     */
    @Override
    public ItineraryDTO getItinerary(Long itineraryId) {
        log.warn("itinerary-service fallback: getItinerary({}) — returning null", itineraryId);
        return null;
    }
}
