package com.team15.tripplanning.bookingservice.controller;

import com.team15.tripplanning.bookingservice.dto.RevenueReportDTO;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody Booking booking) {
        return ResponseEntity.ok(bookingService.create(booking));
    }

    @PostMapping("/{bookingId}/coupons/{couponId}")
    public ResponseEntity<Booking> applyCoupon(
            @PathVariable Long bookingId,
            @PathVariable Long couponId) {

        return ResponseEntity.ok(
                bookingService.applyCoupon(bookingId, couponId)
        );
    }

    @GetMapping
    public ResponseEntity<List<Booking>> findAll() {
        return ResponseEntity.ok(bookingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @RequestBody Booking booking) {
        return ResponseEntity.ok(bookingService.update(id, booking));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports/revenue")
    public ResponseEntity<RevenueReportDTO> getRevenueReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        return ResponseEntity.ok(
                bookingService.getRevenueReport(startDate, endDate)
        );
    }

    @PutMapping("/{id}/retry")
    public ResponseEntity<Booking> retryBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.retryBooking(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getBookingDetails(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingDetails(id));
    }

    // ===== S5-F2: Cancel Booking Endpoint =====
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String reason = body.get("reason");
        Booking updated = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/coupons/top-used")
    public ResponseEntity<?> getTopUsedCoupons(@RequestParam int limit) {
        return ResponseEntity.ok(bookingService.getTopUsedCoupons(limit));
    }
    @GetMapping("/user/{userId}/summary")
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
}
