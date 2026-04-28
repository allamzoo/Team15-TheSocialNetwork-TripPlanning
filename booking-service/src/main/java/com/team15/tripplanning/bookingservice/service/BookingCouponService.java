package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.BookingCoupon;
import com.team15.tripplanning.bookingservice.model.Coupon;
import com.team15.tripplanning.bookingservice.repository.BookingCouponRepository;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.CouponRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingCouponService {
    private final BookingCouponRepository bookingCouponRepository;
    private final BookingRepository bookingRepository;
    private final CouponRepository couponRepository;

    public BookingCouponService(
            BookingCouponRepository bookingCouponRepository,
            BookingRepository bookingRepository,
            CouponRepository couponRepository
    ) {
        this.bookingCouponRepository = bookingCouponRepository;
        this.bookingRepository = bookingRepository;
        this.couponRepository = couponRepository;
    }

    public BookingCoupon create(BookingCoupon bookingCoupon) {
        bookingCoupon.setBooking(resolveBooking(bookingCoupon.getBookingId()));
        bookingCoupon.setCoupon(resolveCoupon(bookingCoupon.getCouponId()));
        return bookingCouponRepository.save(bookingCoupon);
    }

    public List<BookingCoupon> findAll() {
        return bookingCouponRepository.findAll();
    }

    public BookingCoupon findById(Long id) {
        return bookingCouponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BookingCoupon not found: " + id));
    }

    public BookingCoupon update(Long id, BookingCoupon bookingCoupon) {
        BookingCoupon existing = findById(id);
        if (bookingCoupon.getBookingId() != null) {
            existing.setBooking(resolveBooking(bookingCoupon.getBookingId()));
        }
        if (bookingCoupon.getCouponId() != null) {
            existing.setCoupon(resolveCoupon(bookingCoupon.getCouponId()));
        }
        existing.setDiscountApplied(bookingCoupon.getDiscountApplied());
        existing.setAppliedAt(
                bookingCoupon.getAppliedAt() != null ? bookingCoupon.getAppliedAt() : existing.getAppliedAt()
        );
        return bookingCouponRepository.save(existing);
    }

    public void delete(Long id) {
        bookingCouponRepository.delete(findById(id));
    }

    private Booking resolveBooking(Long bookingId) {
        if (bookingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookingId is required for BookingCoupon");
        }
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));
    }

    private Coupon resolveCoupon(Long couponId) {
        if (couponId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "couponId is required for BookingCoupon");
        }
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found: " + couponId));
    }
}
