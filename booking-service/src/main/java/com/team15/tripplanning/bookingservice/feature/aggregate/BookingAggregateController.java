package com.team15.tripplanning.bookingservice.feature.aggregate;

import com.team15.tripplanning.contracts.dto.BookingAggregateRequest;
import com.team15.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.team15.tripplanning.contracts.dto.ItineraryAggregateDTO;
import com.team15.tripplanning.contracts.dto.UserBookingTotalDTO;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingAggregateController {

    private final BookingAggregateService bookingAggregateService;

    public BookingAggregateController(BookingAggregateService bookingAggregateService) {
        this.bookingAggregateService = bookingAggregateService;
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<UserBookingTotalDTO> getUserTotal(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(bookingAggregateService.getUserTotal(userId, startDate, endDate));
    }

    @PostMapping("/aggregate-by-itineraries")
    public ResponseEntity<ItineraryAggregateDTO> aggregateByItineraries(
            @RequestBody BookingAggregateRequest request) {
        return ResponseEntity.ok(bookingAggregateService.aggregateByItineraries(request));
    }

    @GetMapping("/itinerary/{itineraryId}/confirmed-summary")
    public ResponseEntity<ConfirmedSummaryDTO> getConfirmedSummary(@PathVariable Long itineraryId) {
        return ResponseEntity.ok(bookingAggregateService.getConfirmedSummary(itineraryId));
    }
}

