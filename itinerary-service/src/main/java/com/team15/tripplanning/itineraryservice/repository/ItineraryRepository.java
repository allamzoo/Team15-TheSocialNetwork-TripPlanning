package com.team15.tripplanning.itineraryservice.repository;

import com.team15.tripplanning.itineraryservice.model.Itinerary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    List<Itinerary> findByUserId(Long userId);

    List<Itinerary> findByStatus(Itinerary.ItineraryStatus status);

    @Query(value = """
            SELECT i.id, i.title, d.name AS destination_name, COUNT(b.id) AS bookings_count
            FROM itineraries i
            LEFT JOIN destinations d ON d.id = i.destination_id
            LEFT JOIN bookings b ON b.itinerary_id = i.id
            WHERE i.id = :itineraryId
            GROUP BY i.id, i.title, d.name
            """, nativeQuery = true)
    List<Object[]> findItinerarySummaryWithDestinationAndBookings(@Param("itineraryId") Long itineraryId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE itineraries SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    // S3-F1
    @Query(value = """
            SELECT * FROM itineraries
            WHERE start_date >= :startDate
            AND start_date <= :endDate
            AND (:status IS NULL OR status = :status)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Itinerary> searchByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    // S3-F2
    @Query(value = "SELECT COUNT(*) FROM destinations WHERE id = :destinationId", nativeQuery = true)
    int countDestinationById(@Param("destinationId") Long destinationId);

    @Query(value = "SELECT COUNT(*) FROM destinations WHERE id = :destinationId AND status = 'ACTIVE'", nativeQuery = true)
    int countActiveDestinationById(@Param("destinationId") Long destinationId);

    // S3-F3
    @Query(value = """
            SELECT COUNT(*) FROM itineraries
            WHERE destination_id = :destinationId
            AND status IN ('DRAFT', 'PLANNED', 'IN_PROGRESS')
            """, nativeQuery = true)
    int countActiveItinerariesForDestination(@Param("destinationId") Long destinationId);


    @Query(
            value = "SELECT COALESCE(SUM(b.amount), 0) FROM bookings b WHERE b.itinerary_id = :itineraryId AND b.status = 'CONFIRMED'",
            nativeQuery = true
    )
    Double sumConfirmedBookingsByItinerary(@Param("itineraryId") Long itineraryId);
}