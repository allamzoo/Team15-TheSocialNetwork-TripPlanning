package com.team15.tripplanning.destinationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.team15.tripplanning.destinationservice.model.Destination;

@Repository
public interface DestinationDashboardRepository extends JpaRepository<Destination, Long> {

    /**
     * Returns [totalItineraries, completedItineraries, totalVisitors, cancelledItineraries, totalRevenue, totalBookings]
     * for a given destinationId. Joined directly on the shared PG database.
     */
    @Query(value = """
        SELECT
            COUNT(i.id)                                                              AS total_itineraries,
            COUNT(i.id) FILTER (WHERE i.status = 'COMPLETED')                       AS completed_itineraries,
            COUNT(DISTINCT i.user_id)                                                AS total_visitors,
            COUNT(i.id) FILTER (WHERE i.status = 'CANCELLED')                       AS cancelled_itineraries,
            COALESCE(SUM(b.amount) FILTER (WHERE b.status = 'CONFIRMED'), 0)        AS total_revenue,
            COUNT(b.id) FILTER (WHERE b.status = 'CONFIRMED')                       AS total_bookings
        FROM itineraries i
        LEFT JOIN bookings b ON b.itin_id = i.id
        WHERE i.destination_id = :destinationId
        """, nativeQuery = true)
    Object[] getDashboardAggregates(@Param("destinationId") Long destinationId);
}
