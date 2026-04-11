package com.team15.tripplanning.destinationservice.dto;

public class TopDestinationDTO {
    private Long destinationId;
    private String name;
    private Double rating;
    private Long totalBookings;

    public TopDestinationDTO() {
    }

    public TopDestinationDTO(Long destinationId, String name, Double rating, Long totalBookings) {
        this.destinationId = destinationId;
        this.name = name;
        this.rating = rating;
        this.totalBookings = totalBookings;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }
}