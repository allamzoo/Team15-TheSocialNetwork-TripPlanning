package com.team15.tripplanning.itineraryservice.repository;

import com.team15.tripplanning.itineraryservice.model.ItineraryDay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {
    List<ItineraryDay> findByItinerary_IdOrderByDayOrderAsc(Long itineraryId);

    List<ItineraryDay> findByStatus(ItineraryDay.ItineraryDayStatus status);

    @Query(value = """
            SELECT idy.id, idy.day_order, idy.title, COUNT(a.id) AS activities_count
            FROM itinerary_days idy
            LEFT JOIN activities a ON a.itinerary_id = idy.itinerary_id
            WHERE idy.itinerary_id = :itineraryId
            GROUP BY idy.id, idy.day_order, idy.title
            ORDER BY idy.day_order ASC
            """, nativeQuery = true)
    List<Object[]> findDaySummariesWithActivityCount(@Param("itineraryId") Long itineraryId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM itinerary_days WHERE itinerary_id = :itineraryId", nativeQuery = true)
    int deleteAllByItineraryId(@Param("itineraryId") Long itineraryId);
}
