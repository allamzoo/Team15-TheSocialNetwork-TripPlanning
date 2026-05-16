package com.team15.tripplanning.bookingservice.dto;

import java.math.BigDecimal;

public class SettlementProcessRequest {
    private Long itineraryId;
    private Long userId;
    private BigDecimal amount;

    public Long getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(Long itineraryId) {
        this.itineraryId = itineraryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

