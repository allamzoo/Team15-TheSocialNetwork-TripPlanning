package com.team15.tripplanning.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserTripSummaryDTO {
    private Long userId;
    private String name;
    private Long totalTrips;

    @JsonProperty("completedItineraries")
    private Long completedTrips;

    @JsonProperty("cancelledItineraries")
    private Long cancelledTrips;

    private Double totalSpent;
    private Double averageBudget;

    private UserTripSummaryDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UserTripSummaryDTO dto = new UserTripSummaryDTO();

        public Builder userId(Long userId) {
            dto.userId = userId;
            return this;
        }

        public Builder name(String name) {
            dto.name = name;
            return this;
        }

        public Builder totalTrips(Long totalTrips) {
            dto.totalTrips = totalTrips;
            return this;
        }

        public Builder completedTrips(Long completedTrips) {
            dto.completedTrips = completedTrips;
            return this;
        }

        public Builder cancelledTrips(Long cancelledTrips) {
            dto.cancelledTrips = cancelledTrips;
            return this;
        }

        public Builder totalSpent(Double totalSpent) {
            dto.totalSpent = totalSpent;
            return this;
        }

        public Builder averageBudget(Double averageBudget) {
            dto.averageBudget = averageBudget;
            return this;
        }

        public UserTripSummaryDTO build() {
            return dto;
        }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getTotalTrips() { return totalTrips; }
    public void setTotalTrips(Long totalTrips) { this.totalTrips = totalTrips; }

    @JsonProperty("completedItineraries")
    public Long getCompletedTrips() { return completedTrips; }
    public void setCompletedTrips(Long completedTrips) { this.completedTrips = completedTrips; }

    @JsonProperty("cancelledItineraries")
    public Long getCancelledTrips() { return cancelledTrips; }
    public void setCancelledTrips(Long cancelledTrips) { this.cancelledTrips = cancelledTrips; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public Double getAverageBudget() { return averageBudget; }
    public void setAverageBudget(Double averageBudget) { this.averageBudget = averageBudget; }
}
