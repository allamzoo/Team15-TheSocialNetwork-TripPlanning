package com.team15.tripplanning.activityservice.dto;

import java.time.LocalDateTime;

public class ActivitySummaryDTO {
    private Long itineraryId;
    private int totalActivities;
    private Double averageCost;
    private Double maxCost;
    private LocalDateTime firstScheduledTime;
    private LocalDateTime lastScheduledTime;

    private ActivitySummaryDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ActivitySummaryDTO dto = new ActivitySummaryDTO();

        public Builder itineraryId(Long itineraryId) { dto.itineraryId = itineraryId; return this; }
        public Builder totalActivities(int totalActivities) { dto.totalActivities = totalActivities; return this; }
        public Builder averageCost(Double averageCost) { dto.averageCost = averageCost; return this; }
        public Builder maxCost(Double maxCost) { dto.maxCost = maxCost; return this; }
        public Builder firstScheduledTime(LocalDateTime t) { dto.firstScheduledTime = t; return this; }
        public Builder lastScheduledTime(LocalDateTime t) { dto.lastScheduledTime = t; return this; }

        public ActivitySummaryDTO build() { return dto; }
    }

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public int getTotalActivities() { return totalActivities; }
    public void setTotalActivities(int totalActivities) { this.totalActivities = totalActivities; }

    public Double getAverageCost() { return averageCost; }
    public void setAverageCost(Double averageCost) { this.averageCost = averageCost; }

    public Double getMaxCost() { return maxCost; }
    public void setMaxCost(Double maxCost) { this.maxCost = maxCost; }

    public LocalDateTime getFirstScheduledTime() { return firstScheduledTime; }
    public void setFirstScheduledTime(LocalDateTime firstScheduledTime) { this.firstScheduledTime = firstScheduledTime; }

    public LocalDateTime getLastScheduledTime() { return lastScheduledTime; }
    public void setLastScheduledTime(LocalDateTime lastScheduledTime) { this.lastScheduledTime = lastScheduledTime; }
}
