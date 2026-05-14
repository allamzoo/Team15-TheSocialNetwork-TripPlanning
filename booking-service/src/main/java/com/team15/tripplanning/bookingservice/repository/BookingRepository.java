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

    @Modifying
    @Transactional
    @Query(value = "UPDATE bookings SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

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
            SELECT b.type, SUM(b.amount)
            FROM Booking b
            WHERE b.userId = :userId AND b.status = 'CONFIRMED'
            GROUP BY b.type
            """)
    List<Object[]> getBookingSummaryByUser(@Param("userId") Long userId);

    @Query(value = "SELECT status FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    String getItineraryStatus(@Param("itineraryId") Long itineraryId);

    @Query(value = "SELECT COUNT(*) FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    int countItineraryById(@Param("itineraryId") Long itineraryId);

    @Query(value = "SELECT user_id FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    Long getItineraryUserId(@Param("itineraryId") Long itineraryId);

    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId", nativeQuery = true)
    int countUserById(@Param("userId") Long userId);

    // S5-F10 — local-only query (no JOIN to external tables); enrichment via Feign
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
            @Param("endDate") LocalDateTime endDate
    );

    // ─── M3 new endpoints ────────────────────────────────────────────────────

    /** S1-F6: total CONFIRMED booking amount + count for a user in a date range. */
    @Query(value = """
        SELECT COALESCE(SUM(b.amount), 0), COUNT(*)
        FROM bookings b
        WHERE b.user_id   = :userId
          AND b.status    = 'CONFIRMED'
          AND b.created_at BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    Object[] getUserBookingTotal(
            @Param("userId")    Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate
    );

    /** S2-F3 / S3-F4: batch aggregate across a list of itinerary IDs. */
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

    /** S3-F4 saga pre-check + budget calculation: CONFIRMED count + sum for one itinerary. */
    @Query(value = """
        SELECT COUNT(*), COALESCE(SUM(b.amount), 0)
        FROM bookings b
        WHERE b.itin_id = :itineraryId
          AND b.status  = 'CONFIRMED'
        """, nativeQuery = true)
    Object[] getConfirmedSummaryForItinerary(@Param("itineraryId") Long itineraryId);
}