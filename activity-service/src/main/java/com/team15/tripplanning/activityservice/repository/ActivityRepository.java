package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.Activity;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByItineraryId(Long itineraryId);

    List<Activity> findByCategory(Activity.ActivityCategory category);

    @Query(value = """
            SELECT a.id, a.name, a.category, i.title AS itinerary_title
            FROM activities a
            LEFT JOIN itineraries i ON i.id = a.itinerary_id
            WHERE a.itinerary_id = :itineraryId
            """, nativeQuery = true)
    List<Object[]> findActivitiesWithItineraryTitle(@Param("itineraryId") Long itineraryId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM activities WHERE itinerary_id = :itineraryId", nativeQuery = true)
    int deleteAllByItineraryId(@Param("itineraryId") Long itineraryId);

    // -------------------- S4-F1 --------------------
    @Query(value = "SELECT COUNT(*) > 0 FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    boolean itineraryExists(@Param("itineraryId") Long itineraryId);

    Optional<Activity> findFirstByItineraryIdOrderByScheduledTimeDesc(Long itineraryId);

    // -------------------- S4-F8 --------------------
    @Query(value = """
    SELECT 
        COUNT(*) AS totalActivities,
        AVG(CAST(metadata->>'cost' AS numeric)) AS averageCost,
        MAX(CAST(metadata->>'cost' AS numeric)) AS maxCost,
        MIN(scheduled_time) AS firstScheduledTime,
        MAX(scheduled_time) AS lastScheduledTime
    FROM activities
    WHERE itinerary_id = :itineraryId
      AND scheduled_time BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
    List<Object[]> getActivitySummary(
            @Param("itineraryId") Long itineraryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = "SELECT COUNT(*) FROM itineraries WHERE id = :id", nativeQuery = true)
    int countItineraryById(@Param("id") Long id);
}
