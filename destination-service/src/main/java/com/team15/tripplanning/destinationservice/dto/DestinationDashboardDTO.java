package com.team15.tripplanning.destinationservice.dto;

public class DestinationDashboardDTO {

    private Long destinationId;
    private String name;
    private Long totalItineraries;
    private Long completedItineraries;
    private Long totalVisitors;
    private Integer totalRatings;
    private Double averageRating;

    // Private constructor — only Builder can create instances
    private DestinationDashboardDTO() {}

    // Getters
    public Long getDestinationId()         { return destinationId; }
    public String getName()                { return name; }
    public Long getTotalItineraries()      { return totalItineraries; }
    public Long getCompletedItineraries()  { return completedItineraries; }
    public Long getTotalVisitors()         { return totalVisitors; }
    public Integer getTotalRatings()       { return totalRatings; }
    public Double getAverageRating()       { return averageRating; }

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
        public DestinationDashboardDTO build() {
            return dto;
        }
    }
}