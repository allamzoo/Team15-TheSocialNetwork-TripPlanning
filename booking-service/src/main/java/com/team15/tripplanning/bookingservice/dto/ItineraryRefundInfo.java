package com.team15.tripplanning.bookingservice.dto;

import java.time.LocalDate;

public class ItineraryRefundInfo {
    private Long itineraryId;
    private String status;
    private LocalDate startDate;

    public ItineraryRefundInfo(Long itineraryId, String status, LocalDate startDate) {
        this.itineraryId = itineraryId;
        this.status = status;
        this.startDate = startDate;
    }

    public Long getItineraryId() {
        return itineraryId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}