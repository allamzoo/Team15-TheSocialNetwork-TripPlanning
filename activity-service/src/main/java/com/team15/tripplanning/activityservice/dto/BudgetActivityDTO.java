package com.team15.tripplanning.activityservice.dto;

import java.time.LocalDateTime;

public class BudgetActivityDTO {
    private Long activityId;
    private String name;
    private String category;
    private Double latitude;
    private Double longitude;
    private Double cost;
    private LocalDateTime scheduledTime;

    private BudgetActivityDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BudgetActivityDTO dto = new BudgetActivityDTO();

        public Builder activityId(Long activityId) { dto.activityId = activityId; return this; }
        public Builder name(String name) { dto.name = name; return this; }
        public Builder category(String category) { dto.category = category; return this; }
        public Builder latitude(Double latitude) { dto.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { dto.longitude = longitude; return this; }
        public Builder cost(Double cost) { dto.cost = cost; return this; }
        public Builder scheduledTime(LocalDateTime scheduledTime) { dto.scheduledTime = scheduledTime; return this; }

        public BudgetActivityDTO build() { return dto; }
    }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
}
