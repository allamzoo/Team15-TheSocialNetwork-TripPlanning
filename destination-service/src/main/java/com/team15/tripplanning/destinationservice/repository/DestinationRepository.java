package com.team15.tripplanning.destinationservice.repository;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import java.util.List;
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
    SELECT * FROM destinations d
    WHERE d.details ->> :key = :value
      AND (:status IS NULL OR d.status = :status)
    """, nativeQuery = true)
    List<Destination> findByDetailAttribute(
            @Param("key") String key,
            @Param("value") String value,
            @Param("status") String status
    );


    // S2-F6 M3: totalBookings uses local total_ratings as proxy — no cross-DB JOIN
    @Query(value = """
    SELECT d.id           AS destination_id,
           d.name         AS name,
           d.rating       AS rating,
           d.total_ratings AS total_bookings
    FROM destinations d
    ORDER BY d.rating DESC, d.total_ratings DESC, d.id ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopRated(@Param("limit") int limit);




}

