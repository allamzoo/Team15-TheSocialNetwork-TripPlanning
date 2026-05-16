package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.dto.AggregateByItinerariesRequest;
import com.team15.tripplanning.bookingservice.dto.AggregateResultDTO;
import com.team15.tripplanning.bookingservice.dto.AppliedCouponDTO;
import com.team15.tripplanning.bookingservice.dto.AuditEventDTO;
import com.team15.tripplanning.bookingservice.dto.BookingDetailsDTO;
import com.team15.tripplanning.bookingservice.dto.CouponUsageDTO;
import com.team15.tripplanning.bookingservice.dto.DestinationSeasonRevenueDTO;
import com.team15.tripplanning.bookingservice.dto.CreateBookingRequest;
import com.team15.tripplanning.bookingservice.dto.RevenueReportDTO;
import com.team15.tripplanning.bookingservice.dto.SaleAuditTrailDTO;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;
import com.team15.tripplanning.bookingservice.dto.UserBookingTotalDTO;
import com.team15.tripplanning.bookingservice.dto.RefundCancellationRequest;
import com.team15.tripplanning.bookingservice.dto.ItineraryRefundInfo;
import com.team15.tripplanning.contracts.feign.DestinationServiceClient;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import com.team15.tripplanning.contracts.dto.DestinationDTO;
import com.team15.tripplanning.contracts.dto.ItineraryDTO;
import com.team15.tripplanning.bookingservice.model.mongo.PaymentAuditEvent;
import com.team15.tripplanning.bookingservice.repository.PaymentAuditEventRepository;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.BookingCoupon;
import com.team15.tripplanning.bookingservice.model.Coupon;
import com.team15.tripplanning.bookingservice.repository.BookingCouponRepository;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.CouponRepository;
import com.team15.tripplanning.shared.observer.EntityObserver;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team15.tripplanning.bookingservice.strategy.*;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingCouponRepository bookingCouponRepository;
    private final CouponRepository couponRepository;
    private final PaymentAuditEventRepository paymentAuditEventRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserServiceClient userServiceClient;
    private final ItineraryServiceClient itineraryServiceClient;
    private final DestinationServiceClient destinationServiceClient;

    public BookingService(BookingRepository bookingRepository,
                          BookingCouponRepository bookingCouponRepository,
                          CouponRepository couponRepository,
                          PaymentAuditEventRepository paymentAuditEventRepository,
                          MongoEventLogger mongoEventLogger,
                          RedisTemplate<String, Object> redisTemplate,
                          UserServiceClient userServiceClient,
                          ItineraryServiceClient itineraryServiceClient,
                          DestinationServiceClient destinationServiceClient) {
        this.bookingRepository = bookingRepository;
        this.bookingCouponRepository = bookingCouponRepository;
        this.couponRepository = couponRepository;
        this.paymentAuditEventRepository = paymentAuditEventRepository;
        this.redisTemplate = redisTemplate;
        this.userServiceClient = userServiceClient;
        this.itineraryServiceClient = itineraryServiceClient;
        this.destinationServiceClient = destinationServiceClient;
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
        deleteWildcard("s5-destination-season::*");

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
            // Feign call to itinerary-service to get the itinerary's owner
            ItineraryDTO itin = fetchItineraryOrThrow(request.getItineraryId());
            userId = itin.userId();
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
        deleteWildcard("s5-destination-season::*");

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
        deleteWildcard("s5-destination-season::*");
        return saved;
    }

    public void delete(Long id) {
        Booking booking = findById(id);
        bookingRepository.delete(booking);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + booking.getUserId());
        deleteWildcard("s5-booking-details::S5::S5-F4::" + id);
        deleteWildcard("s5-revenue-report::*");
        deleteWildcard("s5-destination-season::*");
    }

    // ===== S5-F3: User Booking Summary =====
    @Cacheable(value = "s5-booking-summary", key = "'S5::S5-F3::' + #userId")
    public UserBookingSummaryDTO getUserBookingSummary(Long userId) {
        // Feign call to user-service instead of local cross-service DB query
        try {
            userServiceClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        } catch (FeignException e) {
            log.warn("user-service unavailable for userId={}, proceeding with local data: {}", userId, e.getMessage());
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
        notifyObservers("RETRY_ATTEMPTED", payload);
        notifyObservers("BOOKING_COMPLETED", payload);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + saved.getUserId());
        deleteWildcard("s5-booking-details::S5::S5-F4::" + id);
        deleteWildcard("s5-revenue-report::*");
        deleteWildcard("s5-destination-season::*");

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
        deleteWildcard("s5-destination-season::*");
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

    // ===== S5-F1 =====
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
        deleteWildcard("s5-destination-season::*");

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
        notifyObservers("REFUNDED", payload);
        deleteWildcard("s5-booking-summary::S5::S5-F3::" + saved.getUserId());
        deleteWildcard("s5-booking-details::S5::S5-F4::" + id);
        deleteWildcard("s5-revenue-report::*");
        deleteWildcard("s5-destination-season::*");

        return saved;
    }

    // ===== S5-F10: Revenue by Destination and Season =====
    @Cacheable(value = "s5-destination-season", key = "'S5::S5-F10::' + #startDate + '::' + #endDate")
    public List<DestinationSeasonRevenueDTO> getRevenueByDestinationAndSeason(
            LocalDate startDate, LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusYears(1);
        if (endDate == null) endDate = LocalDate.now();

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must not be after endDate");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59, 999000000);

        // Step 1 — query local bookings table only (no cross-service JOIN)
        List<Object[]> rows = bookingRepository.getRevenueByItinerary(start, end);

        // Step 2 — aggregate per destinationId using Feign calls
        // Map: destinationId → running totals
        Map<Long, double[]> destTotals = new HashMap<>();   // [totalRevenue, surchargeRevenue, peakCount, offPeakCount]
        Map<Long, Long>     destIdByItinerary = new HashMap<>();

        for (Object[] row : rows) {
            long itineraryId   = ((Number) row[0]).longValue();
            double totalRev    = ((Number) row[1]).doubleValue();
            double surchargeRev = ((Number) row[2]).doubleValue();
            long   peakCount   = ((Number) row[3]).longValue();
            long   offPeakCount = ((Number) row[4]).longValue();

            // Feign: get destinationId for this itinerary
            Long destinationId = destIdByItinerary.computeIfAbsent(itineraryId, iid -> {
                try {
                    ItineraryDTO itin = itineraryServiceClient.getItinerary(iid);
                    return itin != null ? itin.destinationId() : null;
                } catch (FeignException e) {
                    log.warn("itinerary-service unavailable for itineraryId={}: {}", iid, e.getMessage());
                    return null;
                }
            });

            if (destinationId == null) continue;

            destTotals.merge(destinationId, new double[]{totalRev, surchargeRev, peakCount, offPeakCount},
                    (existing, incoming) -> new double[]{
                            existing[0] + incoming[0],
                            existing[1] + incoming[1],
                            existing[2] + incoming[2],
                            existing[3] + incoming[3]
                    });
        }

        // Step 3 — enrich with destination names via Feign
        List<DestinationSeasonRevenueDTO> result = new ArrayList<>();
        for (Map.Entry<Long, double[]> entry : destTotals.entrySet()) {
            Long destinationId = entry.getKey();
            double[] totals    = entry.getValue();

            String destinationName = "Unknown";
            try {
                DestinationDTO dest = destinationServiceClient.getDestination(destinationId);
                if (dest != null && dest.name() != null) {
                    destinationName = dest.name();
                }
            } catch (FeignException e) {
                log.warn("destination-service unavailable for destinationId={}: {}", destinationId, e.getMessage());
            }

            result.add(DestinationSeasonRevenueDTO.builder()
                    .destinationId(destinationId)
                    .destinationName(destinationName)
                    .totalRevenue(totals[0])
                    .surchargeRevenue(totals[1])
                    .baseRevenue(totals[0] - totals[1])
                    .peakBookingCount((long) totals[2])
                    .offPeakBookingCount((long) totals[3])
                    .build());
        }

        result.sort((a, b) -> Double.compare(b.getTotalRevenue(), a.getTotalRevenue()));
        return result;
    }

    // S5-F10 — log ANALYTICS_VIEWED on every call including cache hits
    public void logAnalyticsViewed(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", 0L);
        payload.put("amount", 0.0);
        payload.put("method", "ANALYTICS");
        payload.put("startDate", startDate != null ? startDate.toString() : "");
        payload.put("endDate", endDate != null ? endDate.toString() : "");
        notifyObservers("ANALYTICS_VIEWED", payload);
    }

    // ===== S5-F11: Payment Audit History per Booking =====
    public com.team15.tripplanning.bookingservice.dto.PaginatedPaymentHistoryDTO getPaymentHistory(
            Long bookingId, int page, int size) {
        findById(bookingId);

        // Log ANALYTICS_VIEWED event
        Map<String, Object> viewPayload = new HashMap<>();
        viewPayload.put("bookingId", bookingId);
        viewPayload.put("amount", 0.0);
        viewPayload.put("method", "PAYMENT_HISTORY_VIEW");
        notifyObservers("ANALYTICS_VIEWED", viewPayload);

        // Retrieve all events, filter out ANALYTICS_VIEWED, then paginate in-memory
        List<PaymentAuditEvent> allEvents = paymentAuditEventRepository
                .findByBookingIdOrderByTimestampDesc(bookingId);

        List<PaymentAuditEvent> filtered = allEvents.stream()
                .filter(e -> {
                    String et = e.effectiveEventType();
                    return !"ANALYTICS_VIEWED".equals(et);
                })
                .toList();

        long totalElements = filtered.size();
        int fromIndex = Math.min(page * size, (int) totalElements);
        int toIndex   = Math.min(fromIndex + size, (int) totalElements);
        List<PaymentAuditEvent> pageEvents = filtered.subList(fromIndex, toIndex);

        List<com.team15.tripplanning.bookingservice.dto.PaymentAuditEventDTO> dtos = new ArrayList<>();
        for (PaymentAuditEvent e : pageEvents) {
            dtos.add(com.team15.tripplanning.bookingservice.dto.PaymentAuditEventDTO.builder()
                    .action(e.getAction())
                    .eventType(e.effectiveEventType())
                    .timestamp(e.getTimestamp())
                    .amount(e.getAmount())
                    .method(e.getMethod())
                    .details(e.getDetails())
                    .build());
        }

        return com.team15.tripplanning.bookingservice.dto.PaginatedPaymentHistoryDTO.builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .build();
    }

    @Cacheable(value = "s5-booking-audit", key = "'S5::S5-F11::' + #bookingId")
    public SaleAuditTrailDTO getAuditTrail(Long bookingId) {
        findById(bookingId);
        List<PaymentAuditEvent> events = paymentAuditEventRepository
                .findByBookingIdAndActionNotOrderByTimestampAsc(bookingId, "ANALYTICS_VIEWED");
        List<AuditEventDTO> eventDTOs = new ArrayList<>();
        for (PaymentAuditEvent e : events) {
            eventDTOs.add(AuditEventDTO.builder()
                    .action(e.getAction())
                    .timestamp(e.getTimestamp())
                    .method(e.getMethod())
                    .amount(e.getAmount())
                    .details(e.getDetails())
                    .build());
        }
        return SaleAuditTrailDTO.builder()
                .saleId(bookingId)
                .events(eventDTOs)
                .build();
    }

    // ===== S5-READ-DB: extra aggregate endpoints =====

    /** GET /api/bookings/user/{userId}/total — CONFIRMED bookings in date range. */
    public UserBookingTotalDTO getUserBookingTotal(Long userId,
                                                   LocalDateTime startDate,
                                                   LocalDateTime endDate) {
        Object[] row = bookingRepository.getUserBookingTotal(userId, startDate, endDate);
        // result may be wrapped in an outer array by some JDBC drivers
        if (row.length == 1 && row[0] instanceof Object[] nested) {
            row = nested;
        }
        BigDecimal total = new java.math.BigDecimal(row[0].toString());
        long count = ((Number) row[1]).longValue();

        UserBookingTotalDTO dto = new UserBookingTotalDTO();
        dto.setUserId(userId);
        dto.setTotalAmount(total);
        dto.setBookingCount(count);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        return dto;
    }

    /** POST /api/bookings/aggregate-by-itineraries — batch aggregate by itinerary list. */
    public AggregateResultDTO aggregateByItineraries(AggregateByItinerariesRequest request) {
        if (request.getItineraryIds() == null || request.getItineraryIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itineraryIds must not be empty");
        }
        String status = request.getStatus() != null ? request.getStatus().toUpperCase() : "CONFIRMED";
        LocalDateTime start = request.getStartDate() != null
                ? request.getStartDate() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = request.getEndDate() != null
                ? request.getEndDate() : LocalDateTime.now().plusYears(10);

        Object[] row = bookingRepository.aggregateByItineraries(
                request.getItineraryIds(), status, start, end);
        if (row.length == 1 && row[0] instanceof Object[] nested) {
            row = nested;
        }
        long count = ((Number) row[0]).longValue();
        BigDecimal total = new java.math.BigDecimal(row[1].toString());
        return new AggregateResultDTO(count, total);
    }

    /** GET /api/bookings/itinerary/{itineraryId}/confirmed-summary */
    public AggregateResultDTO getConfirmedSummaryForItinerary(Long itineraryId) {
        Object[] row = bookingRepository.getConfirmedSummaryForItinerary(itineraryId);
        if (row.length == 1 && row[0] instanceof Object[] nested) {
            row = nested;
        }
        long count = ((Number) row[0]).longValue();
        BigDecimal total = new java.math.BigDecimal(row[1].toString());
        return new AggregateResultDTO(count, total);
    }

    private void validateItineraryAllowsBooking(Long itineraryId) {
        // Feign call to itinerary-service replaces native SQL against local shadow table
        ItineraryDTO itin = fetchItineraryOrThrow(itineraryId);

        String normalized = itin.status() != null ? itin.status().trim().toUpperCase() : "";
        if (!normalized.equals("PLANNED") && !normalized.equals("IN_PROGRESS")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking is allowed only for PLANNED or IN_PROGRESS itineraries");
        }
    }

    /** Fetches itinerary from itinerary-service via Feign; throws 404 if not found. */
    private ItineraryDTO fetchItineraryOrThrow(Long itineraryId) {
        try {
            ItineraryDTO itin = itineraryServiceClient.getItinerary(itineraryId);
            if (itin == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found: " + itineraryId);
            }
            return itin;
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found: " + itineraryId);
        } catch (FeignException e) {
            log.error("itinerary-service unavailable for itineraryId={}: {}", itineraryId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "itinerary-service unavailable — cannot validate itinerary");
        }
    }

    @Transactional
    public Map<String, Object> processRefundCancellationTier(Long bookingId, RefundCancellationRequest request) {

        // ===== a) Validate reason is not blank =====
        if (request != null && request.getReason() != null && request.getReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason must not be blank");
        }

        // ===== b) Find booking =====
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + bookingId
                ));

        // ===== c) Fetch itinerary info via Feign (S5-F12: replaces findItineraryRefundInfoRaw) =====
        String itineraryStatus = null;
        Long itineraryId = booking.getItineraryId();
        if (itineraryId != null) {
            try {
                ItineraryDTO itin = itineraryServiceClient.getItinerary(itineraryId);
                if (itin != null && itin.status() != null) {
                    itineraryStatus = itin.status();
                }
            } catch (FeignException e) {
                log.warn("itinerary-service unavailable for itineraryId={}, falling back to booking status: {}",
                        itineraryId, e.getMessage());
            }
        }

        // ===== d) Build refund info using BOOKING's startDate =====
        // The booking's startDate (set by test helper) determines days until departure
        LocalDate bookingStartDate = booking.getStartDate();
        if (bookingStartDate == null) {
            // fallback: use booking's createdAt date
            bookingStartDate = booking.getCreatedAt() != null
                    ? booking.getCreatedAt().toLocalDate()
                    : LocalDate.now();
        }

        // Use itinerary status; if not available, use booking status string
        String effectiveStatus = itineraryStatus != null
                ? itineraryStatus
                : (booking.getStatus() != null ? booking.getStatus().name() : "UNKNOWN");

        ItineraryRefundInfo itinerary = new ItineraryRefundInfo(
                itineraryId,
                effectiveStatus,
                bookingStartDate
        );

        // ===== e) Select strategy =====
        RefundStrategySelector selector = new RefundStrategySelector();
        RefundStrategy strategy = selector.select(itinerary);
        long daysBeforeDeparture = selector.calculateDaysBeforeDeparture(itinerary);

        RefundResult result = strategy.calculateRefund(booking, itinerary, request);

        // ===== f) Build response =====
        Map<String, Object> response = new HashMap<>();
        response.put("bookingId", booking.getId());
        response.put("userId", booking.getUserId());
        response.put("itineraryId", itineraryId);
        response.put("originalAmount", booking.getAmount());
        response.put("refundedAmount", result.getRefundAmount());
        response.put("strategy", result.getStrategyName());
        response.put("tier", result.getTier());
        response.put("daysBeforeDeparture", daysBeforeDeparture);
        response.put("reason", request != null ? request.getReason() : null);
        response.put("status", booking.getStatus() != null ? booking.getStatus().name() : null);

        // ===== g) NoRefundStrategy: log but return 2xx with refundedAmount=0 =====
        if (strategy instanceof NoRefundStrategy) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("bookingId", booking.getId());
            payload.put("userId", booking.getUserId());
            payload.put("strategyName", result.getStrategyName());
            payload.put("reason", "trip already started or completed");
            payload.put("itineraryId", itineraryId);
            payload.put("itineraryStatus", effectiveStatus);
            notifyObservers("REFUND_DENIED", payload);
            deleteWildcard("booking-service::S5-F10::*");
            deleteWildcard("booking-service::S5-F11::" + bookingId + "::*");
            deleteWildcard("booking-service::booking::" + bookingId);
            return response;
        }

        // ===== h) Apply refund - update booking =====
        booking.setStatus(Booking.BookingStatus.CANCELLED);

        Map<String, Object> details = booking.getBookingDetails();
        if (details == null) details = new HashMap<>();
        details.put("refundAmount", result.getRefundAmount());
        details.put("tier", result.getTier());
        details.put("strategyName", result.getStrategyName());
        details.put("refundReason", request != null ? request.getReason() : null);
        details.put("daysBeforeDeparture", daysBeforeDeparture);
        details.put("refundedAt", LocalDateTime.now().toString());
        booking.setBookingDetails(details);

        Booking saved = bookingRepository.save(booking);
        response.put("status", saved.getStatus().name());

        // ===== i) Mongo log =====
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", saved.getId());
        payload.put("userId", saved.getUserId());
        payload.put("amount", result.getRefundAmount());
        payload.put("method", "REFUND");
        payload.put("strategyName", result.getStrategyName());
        payload.put("tier", result.getTier());
        payload.put("originalAmount", saved.getAmount());
        payload.put("daysBeforeDeparture", daysBeforeDeparture);
        payload.put("refundReason", request != null ? request.getReason() : null);
        notifyObservers("REFUND_PROCESSED", payload);

        // ===== j) Cache invalidation =====
        deleteWildcard("booking-service::S5-F10::*");
        deleteWildcard("booking-service::S5-F11::" + bookingId + "::*");
        deleteWildcard("booking-service::booking::" + bookingId);

        return response;
    }
}