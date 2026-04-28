package com.team15.tripplanning.bookingservice.controller;

import com.team15.tripplanning.bookingservice.model.BookingCoupon;
import com.team15.tripplanning.bookingservice.service.BookingCouponService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booking-coupons")
public class BookingCouponController {
    private final BookingCouponService bookingCouponService;

    public BookingCouponController(BookingCouponService bookingCouponService) {
        this.bookingCouponService = bookingCouponService;
    }

    @PostMapping
    public ResponseEntity<BookingCoupon> create(@RequestBody BookingCoupon bookingCoupon) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingCouponService.create(bookingCoupon));
    }

    @GetMapping
    public ResponseEntity<List<BookingCoupon>> findAll() {
        return ResponseEntity.ok(bookingCouponService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingCoupon> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingCouponService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingCoupon> update(@PathVariable Long id, @RequestBody BookingCoupon bookingCoupon) {
        return ResponseEntity.ok(bookingCouponService.update(id, bookingCoupon));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingCouponService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

