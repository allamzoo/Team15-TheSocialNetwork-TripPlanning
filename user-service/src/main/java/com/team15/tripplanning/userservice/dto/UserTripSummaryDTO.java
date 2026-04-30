package com.team15.tripplanning.userservice.dto;

public class UserTripSummaryDTO {
    private Long userId;
    private String name;
    private Long totalTrips;
    private Long completedTrips;
    private Long cancelledTrips;
    private Double totalSpent;
    private Double averageBudget;

    public UserTripSummaryDTO() {
    }

    public UserTripSummaryDTO(Long userId, String name, Long totalTrips, Long completedTrips,
                              Long cancelledTrips, Double totalSpent, Double averageBudget) {
        this.userId = userId;
        this.name = name;
        this.totalTrips = totalTrips;
        this.completedTrips = completedTrips;
        this.cancelledTrips = cancelledTrips;
        this.totalSpent = totalSpent;
        this.averageBudget = averageBudget;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private Long totalTrips;
        private Long completedTrips;
        private Long cancelledTrips;
        private Double totalSpent;
        private Double averageBudget;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder totalTrips(Long totalTrips) {
            this.totalTrips = totalTrips;
            return this;
        }

        public Builder completedTrips(Long completedTrips) {
            this.completedTrips = completedTrips;
            return this;
        }

        public Builder cancelledTrips(Long cancelledTrips) {
            this.cancelledTrips = cancelledTrips;
            return this;
        }

        public Builder totalSpent(Double totalSpent) {
            this.totalSpent = totalSpent;
            return this;
        }

        public Builder averageBudget(Double averageBudget) {
            this.averageBudget = averageBudget;
            return this;
        }

        public UserTripSummaryDTO build() {
            return new UserTripSummaryDTO(userId, name, totalTrips, completedTrips, cancelledTrips, totalSpent, averageBudget);
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(Long totalTrips) {
        this.totalTrips = totalTrips;
    }

    public Long getCompletedTrips() {
        return completedTrips;
    }

    public void setCompletedTrips(Long completedTrips) {
        this.completedTrips = completedTrips;
    }

    public Long getCancelledTrips() {
        return cancelledTrips;
    }

    public void setCancelledTrips(Long cancelledTrips) {
        this.cancelledTrips = cancelledTrips;
    }

    public Double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public Double getAverageBudget() {
        return averageBudget;
    }

    public void setAverageBudget(Double averageBudget) {
        this.averageBudget = averageBudget;
    }
}

