package com.team15.tripplanning.userservice.repository;

import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
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
            SELECT *
            FROM users u
            WHERE (
                (:name IS NOT NULL AND TRIM(:name) <> '' AND LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
                OR (:email IS NOT NULL AND TRIM(:email) <> '' AND LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
                OR (:role IS NOT NULL AND TRIM(:role) <> '' AND u.role = :role)
            )
            """, nativeQuery = true)
    List<User> searchUsers(@Param("name") String name, @Param("email") String email, @Param("role") String role);

    @Query(value = """
            SELECT 
                u.id as userId,
                u.name as name,
                COUNT(i.id) as totalTrips,
                SUM(CASE WHEN i.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedTrips,
                SUM(CASE WHEN i.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledTrips,
                COALESCE(SUM(CASE WHEN i.status = 'COMPLETED' THEN i.estimated_budget ELSE 0 END), 0) as totalSpent,
                CASE
                    WHEN SUM(CASE WHEN i.status = 'COMPLETED' THEN 1 ELSE 0 END) = 0 THEN 0
                    ELSE COALESCE(AVG(CASE WHEN i.status = 'COMPLETED' THEN i.estimated_budget END), 0)
                END as averageBudget
            FROM users u
            LEFT JOIN itineraries i ON u.id = i.user_id
            WHERE u.id = :userId
            GROUP BY u.id, u.name
            """, nativeQuery = true)
    List<Object[]> getUserTripSummary(@Param("userId") Long userId);

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
}
