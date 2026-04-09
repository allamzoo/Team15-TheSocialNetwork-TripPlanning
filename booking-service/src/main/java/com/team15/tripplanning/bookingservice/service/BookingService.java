package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.dto.CouponUsageDTO;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingCouponRepository;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;


import com.team15.tripplanning.bookingservice.dto.AppliedCouponDTO;
import com.team15.tripplanning.bookingservice.dto.BookingDetailsDTO;
import com.team15.tripplanning.bookingservice.model.BookingCoupon;

import java.util.HashMap;
import com.team15.tripplanning.bookingservice.dto.CouponUsageDTO;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.LocalDateTime;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingCouponRepository bookingCouponRepository;


    public BookingService(BookingRepository bookingRepository,
                          BookingCouponRepository bookingCouponRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingCouponRepository = bookingCouponRepository;
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

        // a) Find booking → 404
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + id
                ));

        // b) Validate status → 400
        if (booking.getStatus() != Booking.BookingStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only FAILED bookings can be retried"
            );
        }

        // c) Update status
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        // d) Update JSONB bookingDetails
        Map<String, Object> details = booking.getBookingDetails();

        if (details == null) {
            details = new HashMap<>();
        }

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

    @Transactional
    public BookingDetailsDTO getBookingDetails(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + bookingId
                ));

        List<BookingCoupon> bookingCoupons = booking.getBookingCoupons();

        List<AppliedCouponDTO> appliedCoupons = new ArrayList<>();
        double totalDiscount = 0.0;

        for (BookingCoupon bc : bookingCoupons) {
            if (bc.getCoupon() == null) continue;

            AppliedCouponDTO dto = new AppliedCouponDTO();

            dto.setCouponCode(bc.getCoupon().getCode());
            dto.setDiscountType(bc.getCoupon().getDiscountType().name());
            dto.setDiscountApplied(bc.getDiscountApplied());
            dto.setAppliedAt(bc.getAppliedAt());

            totalDiscount += bc.getDiscountApplied() != null ? bc.getDiscountApplied() : 0;

            appliedCoupons.add(dto);
        }

        double finalAmount = booking.getAmount() - totalDiscount;

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

    public List<CouponUsageDTO> getTopUsedCoupons(int limit) {
        List<Object[]> results = bookingCouponRepository.findTopUsedCoupons(limit);

        List<CouponUsageDTO> response = new ArrayList<>();

        for (Object[] row : results) {
            CouponUsageDTO dto = new CouponUsageDTO();

            dto.setCouponId(((Number) row[0]).longValue());
            dto.setCode((String) row[1]);
            dto.setDiscountType((String) row[2]);
            dto.setDiscountValue(((Number) row[3]).doubleValue());
            dto.setTimesUsed(((Number) row[4]).intValue());
            dto.setTotalDiscountGiven(((Number) row[5]).doubleValue());
            dto.setActive((Boolean) row[6]);

            LocalDateTime expiryDate = (LocalDateTime) row[7];

            // compute expired
            boolean expired = expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
            dto.setExpired(expired);

            response.add(dto);
        }

        return response;
    }
}
