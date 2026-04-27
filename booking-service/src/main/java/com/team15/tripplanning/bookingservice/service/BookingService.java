package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.dto.AppliedCouponDTO;
import com.team15.tripplanning.bookingservice.dto.BookingDetailsDTO;
import com.team15.tripplanning.bookingservice.dto.CouponUsageDTO;
import com.team15.tripplanning.bookingservice.dto.RevenueReportDTO;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.BookingCoupon;
import com.team15.tripplanning.bookingservice.model.Coupon;
import com.team15.tripplanning.bookingservice.repository.BookingCouponRepository;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingCouponRepository bookingCouponRepository;
    private final CouponRepository couponRepository;

    public BookingService(BookingRepository bookingRepository,
                          BookingCouponRepository bookingCouponRepository, CouponRepository couponRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingCouponRepository = bookingCouponRepository;
        this.couponRepository = couponRepository;
    }

    // ===== CRUD =====
    public Booking create(Booking booking) {
        return bookingRepository.save(booking);
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + id
                ));
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

    // ===== S5-F3: User Booking Summary =====
    public UserBookingSummaryDTO getUserBookingSummary(Long userId) {

        // 1️⃣ Check user exists (based on bookings)
        List<Booking> userBookings = bookingRepository.findByUserId(userId);
        if (userBookings.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        // 2️⃣ Get grouped data (CONFIRMED only)
        List<Object[]> results = bookingRepository.getBookingSummaryByUser(userId);

        Map<String, Double> typeBreakdown = new HashMap<>();
        int totalBookings = 0;
        double totalAmount = 0.0;

        // 3️⃣ Build map + totalAmount
        for (Object[] row : results) {
            String type = row[0].toString();
            double amount = ((Number) row[1]).doubleValue();

            typeBreakdown.put(type, amount);
            totalAmount += amount;
        }

        // 4️⃣ Count CONFIRMED bookings
        for (Booking b : userBookings) {
            if (b.getStatus() == Booking.BookingStatus.CONFIRMED) {
                totalBookings++;
            }
        }

        // 5️⃣ Return DTO
        return new UserBookingSummaryDTO(
                userId,
                totalBookings,
                totalAmount,
                typeBreakdown
        );
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
    @Transactional
    public void cancelPendingBookingsByItinerary(Long itineraryId) {

        List<Booking> bookings = bookingRepository.findByItineraryId(itineraryId);

        for (Booking booking : bookings) {
            if (booking.getStatus() == Booking.BookingStatus.PENDING) {
                booking.setStatus(Booking.BookingStatus.CANCELLED);
            }
        }

        bookingRepository.saveAll(bookings);
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

    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date after end date");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        Object[] result = bookingRepository.getRevenueStats(start, end);

        // 🔥 FIX: unwrap nested array
        Object[] row;
        if (result.length == 1 && result[0] instanceof Object[]) {
            row = (Object[]) result[0];
        } else {
            row = result;
        }

        double totalRevenue = ((Number) row[0]).doubleValue();
        long totalBookings = ((Number) row[1]).longValue();
        double cancelledAmount = ((Number) row[2]).doubleValue();
        long cancelledCount = ((Number) row[3]).longValue();

        double average = totalBookings == 0 ? 0 : totalRevenue / totalBookings;

        return new RevenueReportDTO(
                totalRevenue,
                totalBookings,
                average,
                cancelledAmount,
                cancelledCount
        );
    }

    // ===== S5-F1: FINAL SAFE LOGIC =====
    public List<Booking> getBookings(String statusStr, LocalDateTime startDateTime, LocalDateTime endDateTime) {

        Booking.BookingStatus status = null;

        if (statusStr != null && !statusStr.isBlank()) {
            status = Booking.BookingStatus.valueOf(statusStr.toUpperCase());
        }

        // Case 1: status + date range
        if (status != null && startDateTime != null && endDateTime != null) {
            return bookingRepository.searchBookings(status, startDateTime, endDateTime);
        }

        //  Case 2: date range only
        if (startDateTime != null && endDateTime != null) {
            return bookingRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
        }

        //  Case 3: status only
        if (status != null) {
            return bookingRepository.findByStatus(status);
        }

        //  Case 4: no filters
        return bookingRepository.findAll();
    }

    @Transactional
    public Booking applyCoupon(Long bookingId, Long couponId) {

        // a) Find booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // b) Validate booking status
        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED
                || booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "cannot apply coupon to a confirmed/cancelled booking"
            );
        }

        // c) Find coupon
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));

        // d) Validate coupon
        if (!coupon.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon not active");
        }
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon expired");
        }
        if (coupon.getCurrentUses() >= coupon.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon usage limit reached");
        }

        // e) Check duplicate coupon on same booking
        if (bookingCouponRepository.existsByBooking_IdAndCoupon_Id(bookingId, couponId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupon already applied");
        }

        // f) Calculate discount
        double discount;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discount = booking.getAmount() * coupon.getDiscountValue() / 100;
        } else {
            discount = coupon.getDiscountValue();
        }
        discount = Math.min(discount, booking.getAmount());

        // g) Create join entity
        BookingCoupon bookingCoupon = new BookingCoupon();
        bookingCoupon.setBooking(booking);
        bookingCoupon.setCoupon(coupon);
        bookingCoupon.setDiscountApplied(discount);

        // h) Update coupon usage
        coupon.setCurrentUses(coupon.getCurrentUses() + 1);

        // i) Save everything
        booking.getBookingCoupons().add(bookingCoupon);
        bookingCouponRepository.save(bookingCoupon);
        couponRepository.save(coupon);
        bookingRepository.save(booking);

        // j) Return updated booking
        return booking;
    }

    @Transactional
    public Booking cancelBooking(Long id, String reason) {

        // 1) Find booking
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + id
                ));

        // 2) Only CONFIRMED bookings can be cancelled
        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only CONFIRMED bookings can be cancelled"
            );
        }

        // 3) Set status to CANCELLED
        booking.setStatus(Booking.BookingStatus.CANCELLED);

        // 4) Update booking details payload
        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) {
            details = new HashMap<>();
        }
        details.put("cancellationReason", reason != null ? reason : "No reason provided");
        details.put("cancelledAt", LocalDateTime.now().toString());
        booking.setBookingDetails(details);

        // 5) Save and return
        return bookingRepository.save(booking);
    }
}

