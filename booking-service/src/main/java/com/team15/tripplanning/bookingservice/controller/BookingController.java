package com.team15.tripplanning.bookingservice.controller;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.service.BookingService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{id}/retry")
    public ResponseEntity<Booking> retryBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.retryBooking(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getBookingDetails(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingDetails(id));
    }

    @GetMapping("/coupons/top-used")
    public ResponseEntity<?> getTopUsedCoupons(@RequestParam int limit) {
        return ResponseEntity.ok(bookingService.getTopUsedCoupons(limit));
    }
    @PutMapping("/itinerary/{itineraryId}/cancel")
    public ResponseEntity<Void> cancelBookingsByItinerary(@PathVariable Long itineraryId) {
        bookingService.cancelPendingBookingsByItinerary(itineraryId);
        return ResponseEntity.ok().build();
    }
}

