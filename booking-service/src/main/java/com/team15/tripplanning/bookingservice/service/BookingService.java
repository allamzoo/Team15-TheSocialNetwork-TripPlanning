package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;

import java.util.HashMap;
import com.team15.tripplanning.bookingservice.dto.AppliedCouponDTO;
import com.team15.tripplanning.bookingservice.dto.BookingDetailsDTO;
import com.team15.tripplanning.bookingservice.model.BookingCoupon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Booking create(Booking booking) {
        return bookingRepository.save(booking);
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
    }

    public Booking update(Long id, Booking booking) {
        Booking existing = findById(id);
        existing.setItineraryId(booking.getItineraryId());
        existing.setUserId(booking.getUserId());
        existing.setAmount(booking.getAmount());
        existing.setType(booking.getType());
        existing.setStatus(booking.getStatus());
        existing.setBookingDetails(booking.getBookingDetails());
        return bookingRepository.save(existing);
    }

    public void delete(Long id) {
        bookingRepository.delete(findById(id));
    }

    @Transactional
    public Booking retryBooking(Long id) {
        // a) Find booking
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));

        // b) Validate status
        if (booking.getStatus() != Booking.BookingStatus.FAILED) {
            throw new RuntimeException("Only FAILED bookings can be retried");
        }

        // c) Update status
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        // d) Update JSONB bookingDetails
        Map<String, Object> details = booking.getBookingDetails();

        if (details == null) {
            details = new HashMap<>();
        }

        // Get retryAttempt safely
        int retryAttempt = 0;
        Object retryObj = details.get("retryAttempt");

        if (retryObj instanceof Integer) {
            retryAttempt = (Integer) retryObj;
        } else if (retryObj instanceof Number) {
            retryAttempt = ((Number) retryObj).intValue();
        }

        retryAttempt++;

        details.put("retryAttempt", retryAttempt);
        details.put("confirmationNumber", "RETRY-" + id + "-" + retryAttempt);

        booking.setBookingDetails(details);

        // e) Save
        return bookingRepository.save(booking);
    }

    public BookingDetailsDTO getBookingDetails(Long bookingId) {
        // a) find booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        List<BookingCoupon> bookingCoupons = booking.getBookingCoupons();

        List<AppliedCouponDTO> appliedCoupons = new ArrayList<>();

        double totalDiscount = 0.0;

        for (BookingCoupon bc : bookingCoupons) {
            AppliedCouponDTO dto = new AppliedCouponDTO();

            // access Coupon entity
            dto.setCouponCode(bc.getCoupon().getCode());
            dto.setDiscountType(bc.getCoupon().getDiscountType().name());
            dto.setDiscountApplied(bc.getDiscountApplied());
            dto.setAppliedAt(bc.getAppliedAt());

            totalDiscount += bc.getDiscountApplied();

            appliedCoupons.add(dto);
        }

        double finalAmount = booking.getAmount() - totalDiscount;

        // build response DTO
        BookingDetailsDTO result = new BookingDetailsDTO();

        result.setBookingId(booking.getId());
        result.setItineraryId(booking.getItineraryId());
        result.setUserId(booking.getUserId());
        result.setOriginalAmount(booking.getAmount());
        result.setType(booking.getType().name());
        result.setStatus(booking.getStatus().name());
        result.setBookingDetails(booking.getBookingDetails());

        result.setAppliedCoupons(appliedCoupons);
        result.setTotalDiscount(totalDiscount);
        result.setFinalAmount(finalAmount);

        return result;
    }
}
