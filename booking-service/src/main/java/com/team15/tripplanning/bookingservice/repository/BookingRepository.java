package com.team15.tripplanning.bookingservice.repository;

import com.team15.tripplanning.bookingservice.model.Booking;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);
    List<Booking> findByItineraryId(@Param("itineraryId") Long itineraryId);
    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query(value = """
            SELECT b.id, b.amount, b.status, i.title AS itinerary_title
            FROM bookings b
            LEFT JOIN itineraries i ON i.id = b.itin_id
            WHERE b.user_id = :userId
            """, nativeQuery = true)
    List<Object[]> findBookingsWithItineraryTitleByUser(@Param("userId") Long userId);

    // ─── Status updates ───────────────────────────────────────────────────────

    @Modifying
    @Transactional
    @Query(value = "UPDATE bookings SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE bookings SET status = :newStatus WHERE id = :id AND status = :expectedStatus", nativeQuery = true)
    int updateStatusByIdAndStatus(
            @Param("id") Long id,
            @Param("newStatus") String newStatus,
            @Param("expectedStatus") String expectedStatus
    );

    // ─── Revenue / stats ──────────────────────────────────────────────────────

    @Query("""
            SELECT
                COALESCE(SUM(CASE WHEN b.status = 'CONFIRMED' THEN b.amount ELSE 0 END), 0),
                COUNT(CASE WHEN b.status = 'CONFIRMED' THEN 1 END),
                COALESCE(SUM(CASE WHEN b.status = 'CANCELLED' THEN b.amount ELSE 0 END), 0),
                COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END)
            FROM Booking b
            WHERE b.createdAt BETWEEN :startDate AND :endDate
            """)
    Object[] getRevenueStats(LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = :status
              AND b.createdAt BETWEEN :startDateTime AND :endDateTime
            ORDER BY b.createdAt DESC
            """)
    List<Booking> searchBookings(
            @Param("status") Booking.BookingStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    List<Booking> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = 'CONFIRMED'
              AND b.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    List<Booking> findConfirmedByCreatedAtBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
            SELECT b.type, SUM(b.amount)
            FROM Booking b
            WHERE b.userId = :userId AND b.status = 'CONFIRMED'
            GROUP BY b.type
            """)
    List<Object[]> getBookingSummaryByUser(@Param("userId") Long userId);

    // ─── Cross-table validation queries (native) ──────────────────────────────

    @Query(value = "SELECT status FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    String getItineraryStatus(@Param("itineraryId") Long itineraryId);

    @Query(value = "SELECT COUNT(*) FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    int countItineraryById(@Param("itineraryId") Long itineraryId);

    @Query(value = "SELECT user_id FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    Long getItineraryUserId(@Param("itineraryId") Long itineraryId);

    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId", nativeQuery = true)
    int countUserById(@Param("userId") Long userId);

    @Query(value = """
            SELECT i.id, i.status, i.start_date
            FROM itineraries i
            WHERE i.id = :itineraryId
            """, nativeQuery = true)
    Object[] findItineraryRefundInfoRaw(@Param("itineraryId") Long itineraryId);

    // ─── Per-itinerary aggregates ─────────────────────────────────────────────

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM bookings WHERE itin_id = :itineraryId AND status = 'CONFIRMED'", nativeQuery = true)
    Double sumConfirmedAmountByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query(value = "SELECT COUNT(*) FROM bookings WHERE itin_id = :itineraryId AND status = 'CONFIRMED'", nativeQuery = true)
    long countConfirmedByItineraryId(@Param("itineraryId") Long itineraryId);

    /** S3-F4 saga pre-check + budget calculation: CONFIRMED count + sum for one itinerary. */
    @Query(value = """
            SELECT COUNT(*), COALESCE(SUM(b.amount), 0)
            FROM bookings b
            WHERE b.itin_id = :itineraryId
              AND b.status  = 'CONFIRMED'
            """, nativeQuery = true)
    Object[] getConfirmedSummaryForItinerary(@Param("itineraryId") Long itineraryId);

    /**
     * S5-F4 aggregate query — returns [count, totalRevenue] for CONFIRMED bookings
     * without loading the full entity (avoids the JSON bookingDetails column).
     */
    @Query("""
            SELECT COUNT(b), COALESCE(SUM(b.amount), 0.0)
            FROM Booking b
            WHERE b.itineraryId = :itineraryId
              AND b.status = com.team15.tripplanning.bookingservice.model.Booking.BookingStatus.CONFIRMED
            """)
    List<Object[]> countAndSumConfirmed(@Param("itineraryId") Long itineraryId);

    // ─── Per-user aggregates ──────────────────────────────────────────────────

    /** S1-F6: total CONFIRMED booking amount + count for a user in a date range. */
    @Query(value = """
            SELECT COALESCE(SUM(b.amount), 0), COUNT(*)
            FROM bookings b
            WHERE b.user_id    = :userId
              AND b.status     = 'CONFIRMED'
              AND b.created_at BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    Object[] getUserBookingTotal(
            @Param("userId")    Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate
    );

    @Query("""
            SELECT COALESCE(SUM(b.amount), 0) FROM Booking b
            WHERE b.userId = :userId
              AND b.status = 'CONFIRMED'
              AND b.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    Double sumConfirmedAmountByUserAndRange(
            @Param("userId") Long userId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.userId = :userId
              AND b.status = 'CONFIRMED'
              AND b.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countConfirmedByUserAndRange(
            @Param("userId") Long userId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    // ─── Multi-itinerary batch aggregates ─────────────────────────────────────

    /** S2-F3 / S3-F4: batch aggregate across a list of itinerary IDs (native). */
    @Query(value = """
            SELECT COUNT(*), COALESCE(SUM(b.amount), 0)
            FROM bookings b
            WHERE b.itin_id   IN :itineraryIds
              AND b.status    = :status
              AND b.created_at BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    Object[] aggregateByItineraries(
            @Param("itineraryIds") List<Long> itineraryIds,
            @Param("status")       String status,
            @Param("startDate")    LocalDateTime startDate,
            @Param("endDate")      LocalDateTime endDate
    );

    @Query("""
            SELECT COUNT(b), COALESCE(SUM(b.amount), 0)
            FROM Booking b
            WHERE b.itineraryId IN :itineraryIds
              AND b.createdAt BETWEEN :startDateTime AND :endDateTime
              AND (:status IS NULL OR b.status = :status)
            """)
    Object[] aggregateByItineraryIds(
            @Param("itineraryIds") List<Long> itineraryIds,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("status") Booking.BookingStatus status
    );

    // ─── S5-F10 seasonal revenue breakdown ───────────────────────────────────

    @Query(value = """
            SELECT
                b.itin_id                                                     AS itineraryId,
                COALESCE(SUM(b.amount), 0)                                    AS totalRevenue,
                COALESCE(SUM(
                    CAST(COALESCE(
                        NULLIF(b.booking_details->>'seasonalSurcharge', ''),
                        '0'
                    ) AS NUMERIC)
                ), 0)                                                         AS surchargeRevenue,
                COUNT(CASE WHEN CAST(COALESCE(
                    NULLIF(b.booking_details->>'seasonalSurcharge', ''),
                    '0'
                ) AS NUMERIC) > 0 THEN 1 END)                                AS peakBookingCount,
                COUNT(CASE WHEN CAST(COALESCE(
                    NULLIF(b.booking_details->>'seasonalSurcharge', ''),
                    '0'
                ) AS NUMERIC) = 0 THEN 1 END)                                AS offPeakBookingCount
            FROM bookings b
            WHERE b.status = 'CONFIRMED'
              AND b.created_at >= :startDate
              AND b.created_at <= :endDate
            GROUP BY b.itin_id
            ORDER BY SUM(b.amount) DESC
            """, nativeQuery = true)
    List<Object[]> getRevenueByItinerary(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate
    );
}