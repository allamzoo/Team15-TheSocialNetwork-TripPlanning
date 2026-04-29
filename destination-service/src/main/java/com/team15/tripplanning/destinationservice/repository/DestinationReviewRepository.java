package com.team15.tripplanning.destinationservice.repository;

import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.model.DestinationReviewType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DestinationReviewRepository extends JpaRepository<DestinationReview, Long> {
    List<DestinationReview> findByDestination_Id(Long destinationId);

    List<DestinationReview> findByType(DestinationReviewType type);

    @Query(value = """
            SELECT dr.id, dr.rating, dr.content, u.name
            FROM destination_reviews dr
            LEFT JOIN users u ON u.id = dr.user_id
            WHERE dr.destination_id = :destinationId
            """, nativeQuery = true)
    List<Object[]> findReviewsWithUserNames(@Param("destinationId") Long destinationId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM destination_reviews WHERE destination_id = :destinationId", nativeQuery = true)
    int deleteAllByDestinationId(@Param("destinationId") Long destinationId);
}
