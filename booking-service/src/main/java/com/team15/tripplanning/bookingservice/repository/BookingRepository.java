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

    // S5-F10
    @Query(value = """
        SELECT
            d.id                                                          AS destinationId,
            d.name                                                        AS destinationName,
            COALESCE(SUM(b.amount), 0)                                    AS totalRevenue,
            COALESCE(SUM(
                CAST(COALESCE(
                    NULLIF(b.booking_details->>'seasonalSurcharge', ''),
                    '0'
                ) AS NUMERIC)
            ), 0)                                                         AS surchargeRevenue,
            COALESCE(SUM(b.amount), 0) - COALESCE(SUM(
                CAST(COALESCE(
                    NULLIF(b.booking_details->>'seasonalSurcharge', ''),
                    '0'
                ) AS NUMERIC)
            ), 0)                                                         AS baseRevenue,
            COUNT(CASE WHEN CAST(COALESCE(
                NULLIF(b.booking_details->>'seasonalSurcharge', ''),
                '0'
            ) AS NUMERIC) > 0 THEN 1 END)                                AS peakBookingCount,
            COUNT(CASE WHEN CAST(COALESCE(
                NULLIF(b.booking_details->>'seasonalSurcharge', ''),
                '0'
            ) AS NUMERIC) = 0 THEN 1 END)                                AS offPeakBookingCount
        FROM bookings b
        JOIN itineraries i ON i.id = b.itin_id
        JOIN destinations d ON d.id = i.destination_id
        WHERE b.status = 'CONFIRMED'
          AND b.created_at >= :startDate
          AND b.created_at <= :endDate
        GROUP BY d.id, d.name
        ORDER BY SUM(b.amount) DESC
        """, nativeQuery = true)
    List<Object[]> getRevenueByDestinationAndSeason(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
    SELECT i.id, i.status, i.start_date
    FROM itineraries i
    WHERE i.id = :itineraryId
    """, nativeQuery = true)
    Object[] findItineraryRefundInfoRaw(@Param("itineraryId") Long itineraryId);

    /**
     * S5-F4 aggregate query — returns [count, totalRevenue] for CONFIRMED bookings
     * without loading the full entity (avoids the JSON bookingDetails column).
     * Used by BookingAggregateService.getConfirmedSummary().
     */
    @Query("""
            SELECT COUNT(b), COALESCE(SUM(b.amount), 0.0)
            FROM Booking b
            WHERE b.itineraryId = :itineraryId
              AND b.status = com.team15.tripplanning.bookingservice.model.Booking.BookingStatus.CONFIRMED
            """)
    List<Object[]> countAndSumConfirmed(@Param("itineraryId") Long itineraryId);
}