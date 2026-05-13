package com.team15.tripplanning.destinationservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DestinationDashboardDTO {

    private Long destinationId;
    private String name;
    private Long totalItineraries;
    private Long completedItineraries;
    private Long totalVisitors;
    private Integer totalRatings;
    private Double averageRating;
    private Double totalRevenue;
    private Long cancelledItineraries;
    private Double completionRate;
    private Long totalBookings;

    // Private constructor — only Builder can create instances
    private DestinationDashboardDTO() {}

    // totalOrders is an alias for totalItineraries for compatibility
    @JsonProperty("totalOrders")
    public Long getTotalOrders() { return totalItineraries; }

    // Getters
    public Long getDestinationId()          { return destinationId; }
    public String getName()                 { return name; }
    public Long getTotalItineraries()       { return totalItineraries; }
    public Long getCompletedItineraries()   { return completedItineraries; }
    public Long getCancelledItineraries()   { return cancelledItineraries; }
    public Long getTotalVisitors()          { return totalVisitors; }
    public Integer getTotalRatings()        { return totalRatings; }
    public Double getAverageRating()        { return averageRating; }
    public Double getTotalRevenue()         { return totalRevenue; }
    public Double getCompletionRate()       { return completionRate; }
    public Long getTotalBookings()          { return totalBookings; }

    // Static factory to start building
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DestinationDashboardDTO dto = new DestinationDashboardDTO();

        public Builder destinationId(Long destinationId) {
            dto.destinationId = destinationId;
            return this;
        }
        public Builder name(String name) {
            dto.name = name;
            return this;
        }
        public Builder totalItineraries(Long totalItineraries) {
            dto.totalItineraries = totalItineraries;
            return this;
        }
        public Builder completedItineraries(Long completedItineraries) {
            dto.completedItineraries = completedItineraries;
            return this;
        }
        public Builder cancelledItineraries(Long cancelledItineraries) {
            dto.cancelledItineraries = cancelledItineraries;
            return this;
        }
        public Builder totalVisitors(Long totalVisitors) {
            dto.totalVisitors = totalVisitors;
            return this;
        }
        public Builder totalRatings(Integer totalRatings) {
            dto.totalRatings = totalRatings;
            return this;
        }
        public Builder averageRating(Double averageRating) {
            dto.averageRating = averageRating;
            return this;
        }
        public Builder totalRevenue(Double totalRevenue) {
            dto.totalRevenue = totalRevenue;
            return this;
        }
        public Builder completionRate(Double completionRate) {
            dto.completionRate = completionRate;
            return this;
        }
        public Builder totalBookings(Long totalBookings) {
            dto.totalBookings = totalBookings;
            return this;
        }
        public DestinationDashboardDTO build() {
            return dto;
        }
    }
}
