package com.team15.tripplanning.userservice.dto;

public class TopTravelerDTO {
    private Long userId;
    private String name;
    private Double totalSpent;
    private Long tripCount;

    private TopTravelerDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TopTravelerDTO dto = new TopTravelerDTO();

        public Builder userId(Long userId) {
            dto.userId = userId;
            return this;
        }

        public Builder name(String name) {
            dto.name = name;
            return this;
        }

        public Builder totalSpent(Double totalSpent) {
            dto.totalSpent = totalSpent;
            return this;
        }

        public Builder tripCount(Long tripCount) {
            dto.tripCount = tripCount;
            return this;
        }

        public TopTravelerDTO build() {
            return dto;
        }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public Long getTripCount() { return tripCount; }
    public void setTripCount(Long tripCount) { this.tripCount = tripCount; }
}
