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

    @Query(value = """
            SELECT u.id AS user_id,
                   u.name AS name,
                   COALESCE(SUM(b.amount), 0) AS total_spent,
                   COUNT(b.id) AS trip_count
            FROM users u
            JOIN bookings b ON b.user_id = u.id
            WHERE b.status = 'CONFIRMED'
              AND DATE(b.created_at) BETWEEN :startDate AND :endDate
            GROUP BY u.id, u.name
            ORDER BY total_spent DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopTravelersBySpending(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            @Param("limit") int limit
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);
}
