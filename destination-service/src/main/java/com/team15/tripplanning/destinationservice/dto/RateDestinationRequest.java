package com.team15.tripplanning.destinationservice.dto;

public class RateDestinationRequest {
    private Long itineraryId;
    private Integer rating;

    public Long getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(Long itineraryId) {
        this.itineraryId = itineraryId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
