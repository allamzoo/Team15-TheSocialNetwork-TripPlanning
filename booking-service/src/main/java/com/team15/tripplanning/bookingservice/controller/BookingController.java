package com.team15.tripplanning.bookingservice.controller;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.service.BookingService;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;

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

    // ===== S5-F3: User Booking Summary =====
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<UserBookingSummaryDTO> getUserBookingSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getUserBookingSummary(userId));
    }
}