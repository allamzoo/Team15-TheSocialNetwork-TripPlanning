package com.team15.tripplanning.bookingservice.repository;

import com.team15.tripplanning.bookingservice.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByItineraryId(Long itineraryId);

    /** Used as the idempotency check before inserting a new SETTLEMENT_PENDING row. */
    boolean existsByItineraryId(Long itineraryId);
}
