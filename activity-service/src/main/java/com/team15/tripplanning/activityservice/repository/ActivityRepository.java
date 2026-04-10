package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.Activity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Query(value = "SELECT COUNT(*) > 0 FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    boolean itineraryExists(@Param("itineraryId") Long itineraryId);



    Optional<Activity> findFirstByItineraryIdOrderByScheduledTimeDesc(Long itineraryId);

    @Query(value = """
        SELECT a.id, a.name, a.category, a.latitude, a.longitude,
               (SQRT(POW(a.latitude - :lat, 2) + POW(a.longitude - :lon, 2)) * 111) AS distanceKm
        FROM activities a
        GROUP BY a.id
        HAVING (SQRT(POW(a.latitude - :lat, 2) + POW(a.longitude - :lon, 2)) * 111) <= :radiusKm
        ORDER BY distanceKm ASC
        """, nativeQuery = true)
    List<Object[]> findNearbyActivitiesRaw(@Param("lat") Double lat,
                                           @Param("lon") Double lon,
                                           @Param("radiusKm") Double radiusKm);
    // -------------------- S4-F7 --------------------
    // Count activities older than cutoff
    @Query(value = "SELECT COUNT(*) FROM activities WHERE scheduled_time < :cutoff", nativeQuery = true)
    int countByScheduledTimeBefore(@Param("cutoff") LocalDateTime cutoff);

    // Delete activities older than cutoff
    @Modifying
    @Query(value = "DELETE FROM activities WHERE scheduled_time < :cutoff", nativeQuery = true)
    int deleteByScheduledTimeBefore(@Param("cutoff") LocalDateTime cutoff);
}
