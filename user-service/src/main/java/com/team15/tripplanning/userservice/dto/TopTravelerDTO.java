package com.team15.tripplanning.userservice.dto;

public class TopTravelerDTO {
    private Long userId;
    private String name;
    private Double totalSpent;
    private Long tripCount;

    public TopTravelerDTO() {
    }

    public TopTravelerDTO(Long userId, String name, Double totalSpent, Long tripCount) {
        this.userId = userId;
        this.name = name;
        this.totalSpent = totalSpent;
        this.tripCount = tripCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private Double totalSpent;
        private Long tripCount;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder totalSpent(Double totalSpent) {
            this.totalSpent = totalSpent;
            return this;
        }

        public Builder tripCount(Long tripCount) {
            this.tripCount = tripCount;
            return this;
        }

        public TopTravelerDTO build() {
            return new TopTravelerDTO(userId, name, totalSpent, tripCount);
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

    public Double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public Long getTripCount() {
        return tripCount;
    }

    public void setTripCount(Long tripCount) {
        this.tripCount = tripCount;
    }
}

