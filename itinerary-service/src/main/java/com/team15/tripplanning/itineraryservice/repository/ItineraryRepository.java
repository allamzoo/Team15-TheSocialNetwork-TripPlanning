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

    @Query(value = """
            SELECT i.id, i.title, d.name AS destination_name, COUNT(b.id) AS bookings_count
            FROM itineraries i
            LEFT JOIN destinations d ON d.id = i.destination_id
            LEFT JOIN bookings b ON b.itinerary_id = i.id
            WHERE i.id = :itineraryId
            GROUP BY i.id, i.title, d.name
            """, nativeQuery = true)
    List<Object[]> findItinerarySummaryWithDestinationAndBookings(@Param("itineraryId") Long itineraryId);

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

    @Query(value = "SELECT COUNT(*) FROM destinations WHERE id = :id", nativeQuery = true)
    int countDestinationById(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM destinations WHERE id = :id AND status = 'ACTIVE'", nativeQuery = true)
    int countActiveDestinationById(@Param("id") Long id);

    // S3-F3
    @Query(value = """
            SELECT COUNT(*) FROM itineraries
            WHERE destination_id = :destinationId
            AND status IN ('DRAFT', 'PLANNED', 'IN_PROGRESS', 'COMPLETING', 'PAYMENT_PENDING')
            """, nativeQuery = true)
    int countActiveItinerariesForDestination(@Param("destinationId") Long destinationId);

    @Query(
            value = "SELECT COALESCE(SUM(b.amount), 0) FROM bookings b WHERE b.itinerary_id = :itineraryId AND b.status = 'CONFIRMED'",
            nativeQuery = true
    )
    Double sumConfirmedBookingsByItinerary(@Param("itineraryId") Long itineraryId);

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

    @Modifying
    @Transactional
    @Query(value = "UPDATE bookings SET status = 'CANCELLED' WHERE itinerary_id = :itineraryId AND status = 'PENDING'", nativeQuery = true)
    int cancelPendingBookings(@Param("itineraryId") Long itineraryId);

    @Query(value = """
        SELECT u.id, u.name
        FROM itineraries i
        JOIN users u ON u.id = i.user_id
        WHERE i.id = :itineraryId
        """, nativeQuery = true)
    List<Object[]> findUserInfoForVisit(@Param("itineraryId") Long itineraryId);

    @Query(value = """
        SELECT d.id, d.name, d.country, d.category
        FROM itineraries i
        JOIN destinations d ON d.id = i.destination_id
        WHERE i.id = :itineraryId
        """, nativeQuery = true)
    List<Object[]> findDestinationInfoForVisit(@Param("itineraryId") Long itineraryId);

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

    // S3-F12
    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :id", nativeQuery = true)
    int countUsersById(@Param("id") Long id);

    @Query(value = "SELECT id, name, country, CAST(category AS text) FROM destinations WHERE id IN :ids", nativeQuery = true)
    List<Object[]> findDestinationDetailsByIds(@Param("ids") List<Long> ids);

    // ── M3 aggregate endpoints ────────────────────────────────────────────────

    @Query(value = """
            SELECT
                COUNT(*) AS totalTrips,
                SUM(CASE WHEN status IN ('COMPLETED','COMPLETING','PAYMENT_PENDING','PAID') THEN 1 ELSE 0 END) AS completedTrips,
                SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelledTrips,
                COALESCE(SUM(COALESCE(estimated_budget, 0)), 0) AS totalBudget
            FROM itineraries
            WHERE user_id = :userId
            """, nativeQuery = true)
    List<Object[]> getUserTripSummary(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*) FROM itineraries
            WHERE user_id = :userId
            AND status IN ('DRAFT','PLANNED','IN_PROGRESS','COMPLETING','PAYMENT_PENDING')
            """, nativeQuery = true)
    int countActiveByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*) FROM itineraries
            WHERE user_id = :userId
            AND status IN ('COMPLETED','COMPLETING','PAYMENT_PENDING','PAID')
            """, nativeQuery = true)
    long countCompletedByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*) FROM itineraries
            WHERE destination_id = :destinationId
            AND status IN ('DRAFT','PLANNED','IN_PROGRESS','COMPLETING','PAYMENT_PENDING')
            """, nativeQuery = true)
    int countActiveByDestinationId(@Param("destinationId") Long destinationId);

    @Query(value = """
            SELECT
                COUNT(*) AS totalItineraries,
                SUM(CASE WHEN status IN ('COMPLETED','COMPLETING','PAYMENT_PENDING','PAID') THEN 1 ELSE 0 END) AS completedItineraries,
                COUNT(DISTINCT user_id) AS totalVisitors
            FROM itineraries
            WHERE destination_id = :destinationId
            """, nativeQuery = true)
    List<Object[]> getDestinationDashboardStats(@Param("destinationId") Long destinationId);

    @Query(value = """
            SELECT id FROM itineraries
            WHERE destination_id = :destinationId
            AND start_date >= :startDate AND start_date <= :endDate
            """, nativeQuery = true)
    List<Long> findItineraryIdsByDestinationAndDateRange(
            @Param("destinationId") Long destinationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE itineraries SET status = :newStatus WHERE id = :id AND status = :expectedStatus",
            nativeQuery = true)
    int transitionStatus(@Param("id") Long id,
                         @Param("newStatus") String newStatus,
                         @Param("expectedStatus") String expectedStatus);

    // S2-F12 dashboard aggregate
    @Query(value = "SELECT COUNT(*) FROM itineraries WHERE destination_id = :destinationId", nativeQuery = true)
    long countAllByDestinationId(@Param("destinationId") Long destinationId);

    @Query(value = "SELECT COUNT(*) FROM itineraries WHERE destination_id = :destinationId AND status IN ('COMPLETED', 'PAID')", nativeQuery = true)
    long countCompletedByDestinationId(@Param("destinationId") Long destinationId);

    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM itineraries WHERE destination_id = :destinationId AND status IN ('COMPLETED', 'PAID')", nativeQuery = true)
    long countDistinctVisitorsByDestinationId(@Param("destinationId") Long destinationId);

    // S2-F3 booking revenue (uses itinerary estimated_budget as proxy for revenue)
    @Query(value = """
        SELECT COUNT(*), COALESCE(SUM(estimated_budget), 0)
        FROM itineraries
        WHERE destination_id = :destinationId
        AND start_date >= :startDate
        AND start_date <= :endDate
        AND status IN ('COMPLETED', 'PAID')
        """, nativeQuery = true)
    List<Object[]> getRevenueForDestination(
            @Param("destinationId") Long destinationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}