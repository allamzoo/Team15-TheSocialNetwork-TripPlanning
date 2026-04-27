package com.team15.tripplanning.userservice.repository;

import com.team15.tripplanning.userservice.entity.SavedDestination;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SavedDestinationRepository extends JpaRepository<SavedDestination, Long> {
    List<SavedDestination> findByUser_Id(Long userId);

    java.util.Optional<SavedDestination> findByIdAndUser_Id(Long id, Long userId);

    List<SavedDestination> findByUser_IdAndIsDefaultTrue(Long userId);

    @Query(value = """
            SELECT sd.id, sd.label, sd.destination_name, u.name AS user_name
            FROM saved_destinations sd
            JOIN users u ON u.id = sd.user_id
            WHERE sd.user_id = :userId
            """, nativeQuery = true)
    List<Object[]> findSavedDestinationsWithDestinationData(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM saved_destinations WHERE user_id = :userId", nativeQuery = true)
    int deleteAllByUserId(@Param("userId") Long userId);
}
