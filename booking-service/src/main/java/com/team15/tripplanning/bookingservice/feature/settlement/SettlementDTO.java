package com.team15.tripplanning.bookingservice.feature.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementDTO(
        Long id,
        Long itineraryId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime settledAt
) {}
