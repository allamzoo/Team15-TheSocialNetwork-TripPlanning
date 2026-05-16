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

/**
 * M3: Three cross-service SQL queries removed from this interface:
 *
 * 1. findActivitiesWithItineraryTitle — JOINed the itineraries table:
 *      SELECT a.id, a.name, a.category, i.title FROM activities a
 *      LEFT JOIN itineraries i ON i.id = a.itinerary_id WHERE a.itinerary_id = ?
 *    REMOVED: itineraries is now in itinerary-postgres (separate DB).
 *
 * 2. itineraryExists — queried the itineraries table:
 *      SELECT COUNT(*) > 0 FROM itineraries WHERE id = ?
 *    REMOVED: replaced by Feign call in ActivityService.validateItineraryExists().
 *
 * 3. countItineraryById — queried the itineraries table:
 *      SELECT COUNT(*) FROM itineraries WHERE id = ?
 *    REMOVED: S4-F8 simplified per spec (zero-row result returns 200 with totalActivities=0).
 *
 * Everything else is unchanged from M2.
 */
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByItineraryId(Long itineraryId);

    List<Activity> findByCategory(Activity.ActivityCategory category);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM activities WHERE itinerary_id = :itineraryId", nativeQuery = true)
    int deleteAllByItineraryId(@Param("itineraryId") Long itineraryId);

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

    // -------------------- S4-F3 --------------------
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
    @Query(value = "SELECT COUNT(*) FROM activities WHERE scheduled_time < :cutoff", nativeQuery = true)
    int countByScheduledTimeBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query(value = "DELETE FROM activities WHERE scheduled_time < :cutoff", nativeQuery = true)
    int deleteByScheduledTimeBefore(@Param("cutoff") LocalDateTime cutoff);

    // -------------------- S4-F9 --------------------
    @Query(value = """
    SELECT id, name, category, latitude, longitude,
           CAST(metadata->>'cost' AS numeric) AS cost,
           scheduled_time
    FROM activities
    WHERE CAST(metadata->>'cost' AS numeric) <= :maxCost
      AND scheduled_time >= :since
    ORDER BY CAST(metadata->>'cost' AS numeric) ASC
    """, nativeQuery = true)
    List<Object[]> findBudgetFriendlyActivities(
            @Param("maxCost") Double maxCost,
            @Param("since") LocalDateTime since
    );

    // -------------------- S4-F5 --------------------
    @Query(value = """
         SELECT * FROM activities
         WHERE metadata ->> :key = :value
         """, nativeQuery = true)
    List<Activity> filterByMetadataEqNative(@Param("key") String key, @Param("value") String value);

    @Query(value = """
            SELECT a.* FROM activities a
            WHERE (a.metadata->>:key)::numeric > (:value)::numeric
            """, nativeQuery = true)
    List<Activity> filterByMetadataGtNative(@Param("key") String key, @Param("value") String value);

    @Query(value = """
            SELECT a.* FROM activities a
            WHERE (a.metadata->>:key)::numeric < (:value)::numeric
            """, nativeQuery = true)
    List<Activity> filterByMetadataLtNative(@Param("key") String key, @Param("value") String value);

    // -------------------- S4-F6 --------------------
    @Query(value = """
    SELECT * FROM activities a
    WHERE a.scheduled_time >= :startDate
      AND a.scheduled_time <= :endDate
    ORDER BY a.scheduled_time ASC
    """, nativeQuery = true)
    List<Activity> findActivitiesInDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
    SELECT * FROM activities a
    WHERE a.scheduled_time >= :startDate
      AND a.scheduled_time <= :endDate
      AND a.category = :category
    ORDER BY a.scheduled_time ASC
    """, nativeQuery = true)
    List<Activity> findActivitiesByDateRangeAndCategory(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("category") String category);
}