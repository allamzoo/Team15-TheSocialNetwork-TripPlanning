package com.team15.tripplanning.bookingservice.repository;

import com.team15.tripplanning.bookingservice.model.Settlement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /**
     * Standard lookup — used by idempotent INSERT guard in the itinerary.completed consumer.
     */
    Optional<Settlement> findByItineraryId(Long itineraryId);
    /** Used as the idempotency check before inserting a new SETTLEMENT_PENDING row. */
    boolean existsByItineraryId(Long itineraryId);
    /**
     * SELECT … FOR UPDATE — used exclusively by POST /api/bookings/settlement/process.
     *
     * Acquires a row-level pessimistic write lock so concurrent retries see the
     * correct status and exactly one caller proceeds through the PENDING → PROCESSING
     * transition (§1.5 lifecycle, §7 settlement/process behavior step 1–4).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Settlement s WHERE s.itineraryId = :itineraryId")
    Optional<Settlement> findByItineraryIdForUpdate(@Param("itineraryId") Long itineraryId);
}
