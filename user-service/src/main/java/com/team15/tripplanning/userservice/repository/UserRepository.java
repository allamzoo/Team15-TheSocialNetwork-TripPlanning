package com.team15.tripplanning.userservice.repository;

import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.model.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    List<User> findByStatus(UserStatus status);

    @Query(value = """
            SELECT u.id, u.name, u.email, COUNT(sd.id) AS saved_count
            FROM users u
            LEFT JOIN saved_destinations sd ON sd.user_id = u.id
            GROUP BY u.id, u.name, u.email
            """, nativeQuery = true)
    List<Object[]> findUsersWithSavedDestinationCounts();

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    @Query(value = """
            SELECT COUNT(1)
            FROM itineraries i
            WHERE i.user_id = :userId
              AND i.status IN ('DRAFT', 'PLANNED', 'IN_PROGRESS')
            """, nativeQuery = true)
    long countActiveItinerariesForUser(@Param("userId") Long userId);
}
