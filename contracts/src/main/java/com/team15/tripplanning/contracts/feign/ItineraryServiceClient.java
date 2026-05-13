package com.team15.tripplanning.contracts.feign;

import com.team15.tripplanning.contracts.dto.BatchItineraryRequest;
import com.team15.tripplanning.contracts.dto.DestinationBookingRevenueAggregateDTO;
import com.team15.tripplanning.contracts.dto.DestinationDashboardAggregateDTO;
import com.team15.tripplanning.contracts.dto.ItineraryDTO;
import com.team15.tripplanning.contracts.dto.ItinerarySummaryDTO;
import com.team15.tripplanning.contracts.dto.UserTripSummaryAggregateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for itinerary-service.
 * URL resolved from application.yml: feign.itinerary-service.url = http://itinerary-service:8080
 *
 * Used by: user-service (S1-F3, S1-F4, S1-F9),
 *           destination-service (S2-F3, S2-F4, S2-F7, S2-F12),
 *           activity-service (S4-F2, S4-F4),
 *           booking-service (S5-F4, S5-F10, S5-F12, saga).
 */
@FeignClient(name = "itinerary-service", url = "${feign.itinerary-service.url}")
public interface ItineraryServiceClient {

    // ─── Called by user-service ───────────────────────────────────────────────

    /** S1-F3: {totalTrips, completedTrips, cancelledTrips, totalBudget, averageBudget}. */
    @GetMapping("/api/itineraries/user/{userId}/summary")
    UserTripSummaryAggregateDTO getUserItinerarySummary(@PathVariable Long userId);

    /** S1-F4: count of itineraries with status IN (DRAFT, PLANNED, IN_PROGRESS, COMPLETING, PAYMENT_PENDING). */
    @GetMapping("/api/itineraries/user/{userId}/active-count")
    int getActiveItineraryCount(@PathVariable Long userId);

    /** S1-F9: count of itineraries in STATUS_COMPLETED_FAMILY for this user. */
    @GetMapping("/api/itineraries/user/{userId}/completed-count")
    long getCompletedItineraryCount(@PathVariable Long userId);

    // ─── Called by destination-service ───────────────────────────────────────

    /** S2-F3: revenue aggregate fanned out to booking-service internally. */
    @GetMapping("/api/itineraries/destination/{destinationId}/booking-revenue")
    DestinationBookingRevenueAggregateDTO getDestinationBookingRevenue(
            @PathVariable Long destinationId,
            @RequestParam String startDate,
            @RequestParam String endDate
    );

    /** S2-F4: count of active itineraries for this destination (blocks INACTIVE transition). */
    @GetMapping("/api/itineraries/destination/{destinationId}/active-count")
    int getDestinationActiveCount(@PathVariable Long destinationId);

    /** S2-F12: dashboard aggregate — totalItineraries, completedItineraries, totalVisitors. */
    @GetMapping("/api/itineraries/destination/{destinationId}/dashboard-aggregate")
    DestinationDashboardAggregateDTO getDestinationDashboardAggregate(@PathVariable Long destinationId);

    // ─── Called by multiple services ─────────────────────────────────────────

    /**
     * S2-F7 (rating validation), S4-F2/F4 (existence), S5-F4 (status), S5-F12 (strategy input).
     * Returns full itinerary including status and startDate.
     */
    @GetMapping("/api/itineraries/{itineraryId}")
    ItineraryDTO getItinerary(@PathVariable Long itineraryId);

    /**
     * S5-F10: batch lookup — {itineraryIds:[…]} → [{itineraryId, destinationId, userId, status}].
     * Folds N Feign calls into 1 for grouping bookings by destination.
     */
    @PostMapping("/api/itineraries/batch")
    List<ItinerarySummaryDTO> batchGetItineraries(@RequestBody BatchItineraryRequest request);
}
