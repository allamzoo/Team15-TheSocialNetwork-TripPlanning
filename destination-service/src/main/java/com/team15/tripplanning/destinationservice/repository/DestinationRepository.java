package com.team15.tripplanning.destinationservice.repository;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByCountry(String country);

    List<Destination> findByStatus(DestinationStatus status);

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
}
