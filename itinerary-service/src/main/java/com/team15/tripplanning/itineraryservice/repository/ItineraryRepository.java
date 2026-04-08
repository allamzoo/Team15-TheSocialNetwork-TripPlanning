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
}
