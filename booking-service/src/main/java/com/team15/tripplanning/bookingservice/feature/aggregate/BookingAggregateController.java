package com.team15.tripplanning.bookingservice.feature.aggregate;

import com.team15.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.team15.tripplanning.contracts.dto.UserBookingTotalDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * S5-READ-DB — Feign-callable aggregate endpoints.
 *
 * GET  /api/bookings/itinerary/{itineraryId}/confirmed-summary
 *      → ConfirmedSummaryDTO  (saga pre-check from itinerary-service S3-F4)
 *
 * GET  /api/bookings/user/{userId}/total?startDate=&endDate=
 *      → UserBookingTotalDTO  (called by user-service S1-F6)
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingAggregateController {

    private final BookingAggregateService aggregateService;

    public BookingAggregateController(BookingAggregateService aggregateService) {
        this.aggregateService = aggregateService;
    }

    @GetMapping("/itinerary/{itineraryId}/confirmed-summary")
    public ResponseEntity<ConfirmedSummaryDTO> confirmedSummary(
            @PathVariable Long itineraryId) {
        return ResponseEntity.ok(aggregateService.getConfirmedSummary(itineraryId));
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<UserBookingTotalDTO> userTotal(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(aggregateService.getUserTotal(userId, startDate, endDate));
    }
}
