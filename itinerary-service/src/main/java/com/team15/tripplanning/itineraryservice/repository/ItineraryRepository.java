package com.team15.tripplanning.itineraryservice.repository;

import com.team15.tripplanning.itineraryservice.model.Itinerary;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    List<Itinerary> findByUserId(Long userId);

    List<Itinerary> findByStatus(Itinerary.ItineraryStatus status);


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

    // S3-F3
    @Query(value = """
            SELECT COUNT(*) FROM itineraries
            WHERE destination_id = :destinationId
            AND status IN ('DRAFT', 'PLANNED', 'IN_PROGRESS')
            """, nativeQuery = true)
    int countActiveItinerariesForDestination(@Param("destinationId") Long destinationId);


    @Query(
            value = "SELECT * FROM itineraries WHERE metadata ->> :key = :value",
            nativeQuery = true
    )
    List<Itinerary> findByMetadataKeyValue(@Param("key") String key, @Param("value") String value);

    @Query(value = """
    SELECT
        COUNT(*),
        SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END),
        COALESCE(SUM(estimated_budget), 0),
        COALESCE(AVG(CASE WHEN status = 'COMPLETED' THEN estimated_budget ELSE NULL END), 0)
    FROM itineraries
    WHERE start_date >= :startDate AND start_date <= :endDate
    """, nativeQuery = true)
    List<Object[]> getAnalytics(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );


    @Query(value = """
    SELECT
        i.status,
        COUNT(*),
        SUM(COALESCE(i.estimated_budget, 0))
    FROM itineraries i
    WHERE i.start_date >= :startDate AND i.start_date <= :endDate
    GROUP BY i.status
    """, nativeQuery = true)
    List<Object[]> findStatusCountsAndBudgetSumByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // S3-EVENTS: atomic conditional status transition for saga consumers
    @Modifying
    @Transactional
    @Query(value = "UPDATE itineraries SET status = :newStatus WHERE id = :id AND status = :expectedStatus",
            nativeQuery = true)
    int transitionStatus(@Param("id") Long id,
                         @Param("newStatus") String newStatus,
                         @Param("expectedStatus") String expectedStatus);
}