package com.team15.tripplanning.bookingservice.repository;

import com.team15.tripplanning.bookingservice.model.Settlement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByItineraryId(Long itineraryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Settlement s WHERE s.itineraryId = :itineraryId")
    Optional<Settlement> findByItineraryIdForUpdate(@Param("itineraryId") Long itineraryId);

    @Modifying
    @Transactional
    @Query("UPDATE Settlement s SET s.status = :newStatus WHERE s.id = :id AND s.status = :expectedStatus")
    int updateStatusIfMatches(
            @Param("id") Long id,
            @Param("newStatus") Settlement.SettlementStatus newStatus,
            @Param("expectedStatus") Settlement.SettlementStatus expectedStatus
    );
}
