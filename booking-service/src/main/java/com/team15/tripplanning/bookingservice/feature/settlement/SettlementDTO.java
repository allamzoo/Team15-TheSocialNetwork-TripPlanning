package com.team15.tripplanning.bookingservice.feature.settlement;

import com.team15.tripplanning.bookingservice.model.Settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementDTO(
        Long id,
        Long itineraryId,
        Long userId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime settledAt,
        String failureReason
) {
    public static SettlementDTO from(Settlement s) {
        return new SettlementDTO(
                s.getId(),
                s.getItineraryId(),
                s.getUserId(),
                s.getAmount(),
                s.getStatus().name(),
                s.getCreatedAt(),
                s.getSettledAt(),
                s.getFailureReason()
        );
    }
}
