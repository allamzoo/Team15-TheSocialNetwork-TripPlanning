package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.dto.AppliedCouponDTO;
import com.team15.tripplanning.bookingservice.dto.BookingDetailsDTO;
import com.team15.tripplanning.bookingservice.dto.CouponUsageDTO;
import com.team15.tripplanning.bookingservice.dto.RevenueReportDTO;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;
import com.team15.tripplanning.bookingservice.dto.CreateBookingRequest;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.BookingCoupon;
import com.team15.tripplanning.bookingservice.model.Coupon;
import com.team15.tripplanning.bookingservice.repository.BookingCouponRepository;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.CouponRepository;
import com.team15.tripplanning.shared.observer.EntityObserver;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingCouponRepository bookingCouponRepository;
    private final CouponRepository couponRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final RedisTemplate<String, Object> redisTemplate;

    public BookingService(BookingRepository bookingRepository,
                          BookingCouponRepository bookingCouponRepository,
                          CouponRepository couponRepository,
                          MongoEventLogger mongoEventLogger,
                          RedisTemplate<String, Object> redisTemplate) {
        this.bookingRepository = bookingRepository;
        this.bookingCouponRepository = bookingCouponRepository;
        this.couponRepository = couponRepository;
        this.redisTemplate = redisTemplate;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    private void deleteWildcard(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ===== CRUD =====
    public Booking create(Booking booking) {
        if (booking.getStatus() == null) {
            booking.setStatus(Booking.BookingStatus.PENDING);
        }
        Booking saved = bookingRepository.save(booking);

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", saved.getId());
        payload.put("userId", saved.getUserId());
        payload.put("amount", saved.getAmount());
        payload.put("method", saved.getType() != null ? saved.getType().name() : null);
        notifyObservers("BOOKING_CREATED", payload);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + saved.getUserId());
        deleteWildcard("s5-revenue-report::*");

        return saved;
    }

    @Transactional
    public Booking createFromRequest(CreateBookingRequest request) {
        if (request.getItineraryId() == null
                || request.getAmount() == null || request.getType() == null || request.getType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itineraryId, amount and type are required");
        }

        validateItineraryAllowsBooking(request.getItineraryId());

        Long userId = request.getUserId();
        if (userId == null) {
            userId = bookingRepository.getItineraryUserId(request.getItineraryId());
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }

        Booking booking = new Booking();
        booking.setItineraryId(request.getItineraryId());
        booking.setUserId(userId);
        booking.setAmount(request.getAmount());

        try {
            booking.setType(Booking.BookingType.valueOf(request.getType().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid booking type");
        }

        booking.setStatus(Booking.BookingStatus.PENDING);

        Map<String, Object> details = new HashMap<>();
        if (request.getProviderName() != null && !request.getProviderName().isBlank()) {
            details.put("providerName", request.getProviderName());
        }
        booking.setBookingDetails(details);

        Booking saved = bookingRepository.save(booking);

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", saved.getId());
        payload.put("userId", saved.getUserId());
        payload.put("amount", saved.getAmount());
        payload.put("method", saved.getType() != null ? saved.getType().name() : null);
        notifyObservers("BOOKING_CREATED", payload);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + saved.getUserId());
        deleteWildcard("s5-revenue-report::*");

        return saved;
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
        if (booking.getItineraryId() != null) {
            existing.setItineraryId(booking.getItineraryId());
        }
        if (booking.getUserId() != null) {
            existing.setUserId(booking.getUserId());
        }
        if (booking.getAmount() != null) {
            existing.setAmount(booking.getAmount());
        }
        if (booking.getType() != null) {
            existing.setType(booking.getType());
        }
        if (booking.getStatus() != null) {
            existing.setStatus(booking.getStatus());
        }
        if (booking.getBookingDetails() != null) {
            existing.setBookingDetails(booking.getBookingDetails());
        }
        Booking saved = bookingRepository.save(existing);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + saved.getUserId());
        deleteWildcard("s5-booking-details::S5::S5-F4::" + id);
        deleteWildcard("s5-revenue-report::*");
        return saved;
    }

    public void delete(Long id) {
        Booking booking = findById(id);
        bookingRepository.delete(booking);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + booking.getUserId());
        deleteWildcard("s5-booking-details::S5::S5-F4::" + id);
        deleteWildcard("s5-revenue-report::*");
    }

    // ===== S5-F3: User Booking Summary =====
    @Cacheable(value = "s5-booking-summary", key = "'S5::S5-F3::' + #userId")
    public UserBookingSummaryDTO getUserBookingSummary(Long userId) {
        if (bookingRepository.countUserById(userId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }

        List<Booking> userBookings = bookingRepository.findByUserId(userId);
        List<Object[]> results = bookingRepository.getBookingSummaryByUser(userId);

        Map<String, Double> typeBreakdown = new HashMap<>();
        int totalBookings = 0;
        double totalAmount = 0.0;

        for (Object[] row : results) {
            String type = row[0].toString();
            double amount = ((Number) row[1]).doubleValue();
            typeBreakdown.put(type, amount);
            totalAmount += amount;
        }

        for (Booking b : userBookings) {
            if (b.getStatus() == Booking.BookingStatus.CONFIRMED) {
                totalBookings++;
            }
        }

        return UserBookingSummaryDTO.builder()
                .userId(userId)
                .totalBookings(totalBookings)
                .totalAmount(totalAmount)
                .typeBreakdown(typeBreakdown)
                .build();
    }

    @Transactional
    public Booking retryBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + id
                ));

        if (booking.getStatus() != Booking.BookingStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only FAILED bookings can be retried"
            );
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);

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

        Booking saved = bookingRepository.save(booking);

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", saved.getId());
        payload.put("userId", saved.getUserId());
        payload.put("amount", saved.getAmount());
        notifyObservers("BOOKING_RETRIED", payload);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + saved.getUserId());
        deleteWildcard("s5-booking-details::S5::S5-F4::" + id);
        deleteWildcard("s5-revenue-report::*");

        return saved;
    }

    @Transactional
    @Cacheable(value = "s5-booking-details", key = "'S5::S5-F4::' + #bookingId")
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

        return BookingDetailsDTO.builder()
                .bookingId(booking.getId())
                .itineraryId(booking.getItineraryId())
                .userId(booking.getUserId())
                .originalAmount(booking.getAmount())
                .type(booking.getType().name())
                .status(booking.getStatus().name())
                .bookingDetails(booking.getBookingDetails())
                .appliedCoupons(appliedCoupons)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .build();
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
        deleteWildcard("s5-revenue-report::*");
    }

    @Cacheable(value = "s5-top-coupons", key = "'S5::S5-F5::' + #limit")
    public List<CouponUsageDTO> getTopUsedCoupons(int limit) {
        List<Object[]> results = bookingCouponRepository.findTopUsedCoupons(limit);

        List<CouponUsageDTO> response = new ArrayList<>();

        for (Object[] row : results) {
            LocalDateTime expiryDate = null;
            if (row[7] instanceof LocalDateTime ldt) {
                expiryDate = ldt;
            } else if (row[7] instanceof Timestamp ts) {
                expiryDate = ts.toLocalDateTime();
            }

            boolean expired = expiryDate != null && expiryDate.isBefore(LocalDateTime.now());

            response.add(CouponUsageDTO.builder()
                    .couponId(((Number) row[0]).longValue())
                    .code((String) row[1])
                    .discountType((String) row[2])
                    .discountValue(((Number) row[3]).doubleValue())
                    .timesUsed(((Number) row[4]).intValue())
                    .totalDiscountGiven(((Number) row[5]).doubleValue())
                    .active((Boolean) row[6])
                    .expired(expired)
                    .build());
        }

        return response;
    }

    @Cacheable(value = "s5-revenue-report", key = "'S5::S5-F6::' + #startDate + '::' + #endDate")
    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date after end date");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        Object[] result = bookingRepository.getRevenueStats(start, end);

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

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .averageBookingAmount(average)
                .cancelledAmount(cancelledAmount)
                .cancelledCount(cancelledCount)
                .build();
    }

    // ===== S5-F1: FINAL SAFE LOGIC =====
    public List<Booking> getBookings(String statusStr, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Booking.BookingStatus status = null;

        if (statusStr != null && !statusStr.isBlank()) {
            status = Booking.BookingStatus.valueOf(statusStr.toUpperCase());
        }

        if (status != null && startDateTime != null && endDateTime != null) {
            return bookingRepository.searchBookings(status, startDateTime, endDateTime);
        }

        if (startDateTime != null && endDateTime != null) {
            return bookingRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
        }

        if (status != null) {
            return bookingRepository.findByStatus(status);
        }

        return bookingRepository.findAll();
    }

    @Transactional
    public Booking applyCoupon(Long bookingId, Long couponId) {
        if (bookingId == null || couponId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookingId and couponId are required");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED
                || booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "cannot apply coupon to a confirmed/cancelled booking"
            );
        }

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));

        if (!coupon.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon not active");
        }
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon expired");
        }
        if (coupon.getCurrentUses() >= coupon.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon usage limit reached");
        }

        if (bookingCouponRepository.existsByBooking_IdAndCoupon_Id(bookingId, couponId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupon already applied");
        }

        double discount;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discount = booking.getAmount() * coupon.getDiscountValue() / 100;
        } else {
            discount = coupon.getDiscountValue();
        }
        discount = Math.min(discount, booking.getAmount());

        BookingCoupon bookingCoupon = new BookingCoupon();
        bookingCoupon.setBooking(booking);
        bookingCoupon.setCoupon(coupon);
        bookingCoupon.setDiscountApplied(discount);

        coupon.setCurrentUses(coupon.getCurrentUses() + 1);

        booking.getBookingCoupons().add(bookingCoupon);
        bookingCouponRepository.save(bookingCoupon);
        couponRepository.save(coupon);
        bookingRepository.save(booking);

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", bookingId);
        payload.put("userId", booking.getUserId());
        payload.put("amount", discount);
        payload.put("method", coupon.getDiscountType().name());
        notifyObservers("COUPON_APPLIED", payload);
        deleteWildcard("s5-booking-details::S5::S5-F4::" + bookingId);
        deleteWildcard("s5-top-coupons::*");

        return booking;
    }

    @Transactional
    public Booking cancelBooking(Long id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + id
                ));

        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only CONFIRMED bookings can be cancelled"
            );
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);

        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) {
            details = new HashMap<>();
        }
        details.put("cancellationReason", reason != null ? reason : "No reason provided");
        details.put("cancelledAt", LocalDateTime.now().toString());
        booking.setBookingDetails(details);

        Booking saved = bookingRepository.save(booking);

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", saved.getId());
        payload.put("userId", saved.getUserId());
        payload.put("amount", saved.getAmount());
        payload.put("method", "CANCELLATION");
        notifyObservers("BOOKING_CANCELLED", payload);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + saved.getUserId());
        deleteWildcard("s5-booking-details::S5::S5-F4::" + id);
        deleteWildcard("s5-revenue-report::*");

        return saved;
    }

    private void validateItineraryAllowsBooking(Long itineraryId) {
        if (bookingRepository.countItineraryById(itineraryId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }

        String status = bookingRepository.getItineraryStatus(itineraryId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }

        String normalized = status.trim().toUpperCase();
        if (!normalized.equals("PLANNED") && !normalized.equals("IN_PROGRESS")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking is allowed only for PLANNED or IN_PROGRESS itineraries");
        }
    }
}
