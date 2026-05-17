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




    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM bookings WHERE itin_id = :itineraryId AND status = 'CONFIRMED'", nativeQuery = true)
    Double sumConfirmedAmountByItineraryId(@Param("itineraryId") Long itineraryId);

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

    @Query(value = "SELECT COUNT(*) FROM bookings WHERE itin_id = :itineraryId AND status = 'CONFIRMED'", nativeQuery = true)
    long countConfirmedByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query("""
            SELECT COUNT(b), COALESCE(SUM(b.amount), 0)
            FROM Booking b
            WHERE b.itineraryId = :itineraryId
              AND b.status = 'CONFIRMED'
            """)
    Object[] getConfirmedSummaryByItinerary(@Param("itineraryId") Long itineraryId);

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
}
