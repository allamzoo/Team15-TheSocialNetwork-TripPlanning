package com.team15.tripplanning.bookingservice.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Request body for POST /api/bookings/aggregate-by-itineraries */
public class AggregateByItinerariesRequest {

    private List<Long> itineraryIds;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public AggregateByItinerariesRequest() {}

    public List<Long> getItineraryIds() { return itineraryIds; }
    public void setItineraryIds(List<Long> itineraryIds) { this.itineraryIds = itineraryIds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}
