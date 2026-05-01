package com.team15.tripplanning.bookingservice.controller;

import com.team15.tripplanning.bookingservice.dto.CreateBookingRequest;
import com.team15.tripplanning.bookingservice.dto.RevenueReportDTO;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.service.BookingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.team15.tripplanning.bookingservice.dto.RefundCancellationRequest;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody Booking booking) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(booking));
    }

    @PostMapping("/create")
    public ResponseEntity<Booking> createFromRequest(@RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createFromRequest(request));
    }

    @PostMapping("/itinerary/{itineraryId}")
    public ResponseEntity<Booking> createForItinerary(
            @PathVariable Long itineraryId,
            @RequestBody(required = false) CreateBookingRequest request
    ) {
        if (request == null) {
            request = new CreateBookingRequest();
        }

        request.setItineraryId(itineraryId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createFromRequest(request));
    }

    @PostMapping("/{bookingId}/coupons/{couponId}")
    public ResponseEntity<Booking> applyCoupon(
            @PathVariable Long bookingId,
            @PathVariable Long couponId
    ) {
        return ResponseEntity.ok(bookingService.applyCoupon(bookingId, couponId));
    }

    @GetMapping
    public ResponseEntity<List<Booking>> findAll() {
        return ResponseEntity.ok(bookingService.findAll());
    }

    @PutMapping("/itinerary/{itineraryId}/cancel")
    public ResponseEntity<Void> cancelBookingsByItinerary(@PathVariable Long itineraryId) {
        bookingService.cancelPendingBookingsByItinerary(itineraryId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @RequestBody Booking booking) {
        return ResponseEntity.ok(bookingService.update(id, booking)); // 200
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }
    @GetMapping("/reports/revenue")
    public ResponseEntity<RevenueReportDTO> getRevenueReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(bookingService.getRevenueReport(startDate, endDate));
    }

    @PutMapping("/{id}/retry")
    public ResponseEntity<Booking> retryBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.retryBooking(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getBookingDetails(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingDetails(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : null;
        Booking updated = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBookingPost(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String reason = null;
        if (body != null && body.get("reason") != null) {
            reason = body.get("reason").toString();
        }
        Booking updated = bookingService.cancelBooking(id, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    @GetMapping("/coupons/top-used")
    public ResponseEntity<?> getTopUsedCoupons(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(bookingService.getTopUsedCoupons(limit));
    }

    @GetMapping({"/user/{userId}/summary", "/users/{userId}/summary"})
    public ResponseEntity<UserBookingSummaryDTO> getUserBookingSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getUserBookingSummary(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Booking>> searchBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        List<Booking> bookings = bookingService.getBookings(status, startDateTime, endDateTime);
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{id}/refund-cancellation-tier")
    public ResponseEntity<Booking> refundCancellationTier(
            @PathVariable Long id,
            @RequestBody(required = false) RefundCancellationRequest request
    ) {
        if (request == null) {
            request = new RefundCancellationRequest();
        }

        Booking updated = bookingService.processRefundCancellationTier(id, request);
        return ResponseEntity.ok(updated);
    }
}
