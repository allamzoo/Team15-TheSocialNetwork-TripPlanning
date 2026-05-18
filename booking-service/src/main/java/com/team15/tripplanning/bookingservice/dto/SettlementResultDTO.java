package com.team15.tripplanning.bookingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Response from POST /api/bookings/settlement/process (§7). */
public class SettlementResultDTO {

    private Long settlementId;
    private Long itineraryId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime settledAt;
    private String failureReason;

    public static SettlementResultDTO from(
            Long settlementId,
            Long itineraryId,
            Long userId,
            BigDecimal amount,
            String status,
            LocalDateTime settledAt,
            String failureReason) {
        SettlementResultDTO dto = new SettlementResultDTO();
        dto.setSettlementId(settlementId);
        dto.setItineraryId(itineraryId);
        dto.setUserId(userId);
        dto.setAmount(amount);
        dto.setStatus(status);
        dto.setSettledAt(settledAt);
        dto.setFailureReason(failureReason);
        return dto;
    }

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}