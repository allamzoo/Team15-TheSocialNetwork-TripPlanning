package com.team15.tripplanning.destinationservice.repository;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByCountry(String country);

    List<Destination> findByStatus(DestinationStatus status);

    @Query("""
        SELECT d
        FROM Destination d
        WHERE (:category IS NULL OR d.category = :category)
          AND d.rating >= :minRating
          AND d.rating <= :maxRating
        ORDER BY d.rating DESC
        """)
    List<Destination> searchDestinations(
            @Param("category") DestinationCategory category,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating
    );

    @Query(value = """
        SELECT
            COUNT(b.id) AS total_bookings,
            COALESCE(SUM(b.amount), 0) AS total_revenue,
            COALESCE(AVG(b.amount), 0) AS average_booking_amount
        FROM bookings b
        JOIN itineraries i ON b.itin_id = i.id
        WHERE i.destination_id = :destinationId
          AND b.status = 'CONFIRMED'
          AND DATE(b.created_at) BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    List<Object[]> getDestinationRevenueSummary(
            @Param("destinationId") Long destinationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
            SELECT d.id, d.name, COALESCE(AVG(dr.rating), 0) AS avg_rating, COUNT(dr.id) AS reviews_count
            FROM destinations d
            LEFT JOIN destination_reviews dr ON dr.destination_id = d.id
            GROUP BY d.id, d.name
            """, nativeQuery = true)
    List<Object[]> findDestinationRatingsSummary();

    @Modifying
    @Transactional
    @Query(value = "UPDATE destinations SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    @Query(value = """
    SELECT COUNT(*) FROM itineraries
    WHERE destination_id = :destinationId
      AND status IN ('DRAFT', 'PLANNED', 'IN_PROGRESS')
    """, nativeQuery = true)
    long countActiveItinerariesForDestination(@Param("destinationId") Long destinationId);


    @Query(value = """
    SELECT * FROM destinations d
    WHERE d.details ->> :key = :value
      AND (:status IS NULL OR d.status = :status)
    """, nativeQuery = true)
    List<Destination> findByDetailAttribute(
            @Param("key") String key,
            @Param("value") String value,
            @Param("status") String status
    );


    @Query(value = """
    SELECT d.id                         AS destination_id,
           d.name                       AS name,
           d.rating                     AS rating,
           COALESCE(COUNT(b.id), 0)     AS total_bookings
    FROM destinations d
    LEFT JOIN itineraries i ON i.destination_id = d.id
    LEFT JOIN bookings b    ON b.itin_id = i.id AND b.status = 'CONFIRMED'
    GROUP BY d.id, d.name, d.rating, d.total_ratings
    ORDER BY d.rating DESC, d.total_ratings DESC, d.id ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopRatedWithBookingCount(@Param("limit") int limit);




    // In DestinationRepository.java
    @Query(value = "SELECT COUNT(*) FROM itineraries WHERE id = :itineraryId AND destination_id = :destinationId AND status = 'COMPLETED'", nativeQuery = true)
    int countValidItinerary(@Param("itineraryId") Long itineraryId, @Param("destinationId") Long destinationId);

    @Query(value = "SELECT COUNT(*) FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    int countItineraryById(@Param("itineraryId") Long itineraryId);

    @Query(value = "SELECT role FROM users WHERE id = :userId", nativeQuery = true)
    String findUserRoleById(@Param("userId") Long userId);
}

