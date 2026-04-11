package com.team15.tripplanning.bookingservice.repository;

import com.team15.tripplanning.bookingservice.model.Booking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByItineraryId(Long itineraryId);
    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query(value = """
            SELECT b.id, b.amount, b.status, i.title AS itinerary_title
            FROM bookings b
            LEFT JOIN itineraries i ON i.id = b.itinerary_id
            WHERE b.user_id = :userId
            """, nativeQuery = true)
    List<Object[]> findBookingsWithItineraryTitleByUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE bookings SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);
}
