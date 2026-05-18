package com.team15.tripplanning.contracts.feign;

import com.team15.tripplanning.contracts.dto.BookingAggregateRequest;
import com.team15.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.team15.tripplanning.contracts.dto.ItineraryAggregateDTO;
import com.team15.tripplanning.contracts.dto.UserBookingTotalDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for booking-service.
 * URL resolved from application.yml: feign.booking-service.url = http://booking-service:8080
 *
 * Used by: user-service (S1-F6),
 *           itinerary-service (S3-F4 saga pre-check + budget, S2-F3 revenue chain).
 */
@FeignClient(name = "booking-service", url = "${feign.booking-service.url}")
public interface BookingServiceClient {

    // ─── Called by user-service (S1-F6) ──────────────────────────────────────

    /**
     * S1-F6: sum of CONFIRMED bookings for a user in the given date range.
     * Used to rank top travelers by spending.
     */
    @GetMapping("/api/bookings/user/{userId}/total")
    UserBookingTotalDTO getUserBookingTotal(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate
    );

    // ─── Called by itinerary-service ─────────────────────────────────────────

    /**
     * S3-F4 saga trigger + S2-F3 revenue chain:
     * batch aggregate — {itineraryIds, startDate, endDate, status} → {totalBookings, totalRevenue}.
     */
    @PostMapping("/api/bookings/aggregate-by-itineraries")
    ItineraryAggregateDTO aggregateByItineraries(@RequestBody BookingAggregateRequest request);

    /**
     * S3-F4 saga pre-check:
     * {count, totalRevenue} for CONFIRMED bookings on this itinerary.
     * count >= 1 → itinerary has at least one confirmed booking (saga may proceed).
     * totalRevenue → stored into Itinerary.estimatedBudget.
     */
    @GetMapping("/api/bookings/itinerary/{itineraryId}/confirmed-summary")
    ConfirmedSummaryDTO getConfirmedSummary(@PathVariable Long itineraryId);
}
