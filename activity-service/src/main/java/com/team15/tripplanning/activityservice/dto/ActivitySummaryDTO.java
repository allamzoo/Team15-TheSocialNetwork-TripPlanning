package com.team15.tripplanning.activityservice.dto;
import java.time.LocalDateTime;

public class ActivitySummaryDTO {
    private Long itineraryId;
    private int totalActivities;
    private Double averageCost;
    private Double maxCost;
    private LocalDateTime firstScheduledTime;
    private LocalDateTime lastScheduledTime;

    // Constructor
    public ActivitySummaryDTO(Long itineraryId, int totalActivities, Double averageCost,
                              Double maxCost, LocalDateTime firstScheduledTime,
                              LocalDateTime lastScheduledTime) {
        this.itineraryId = itineraryId;
        this.totalActivities = totalActivities;
        this.averageCost = averageCost;
        this.maxCost = maxCost;
        this.firstScheduledTime = firstScheduledTime;
        this.lastScheduledTime = lastScheduledTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long itineraryId;
        private int totalActivities;
        private Double averageCost;
        private Double maxCost;
        private LocalDateTime firstScheduledTime;
        private LocalDateTime lastScheduledTime;

        public Builder itineraryId(Long itineraryId) {
            this.itineraryId = itineraryId;
            return this;
        }

        public Builder totalActivities(int totalActivities) {
            this.totalActivities = totalActivities;
            return this;
        }

        public Builder averageCost(Double averageCost) {
            this.averageCost = averageCost;
            return this;
        }

        public Builder maxCost(Double maxCost) {
            this.maxCost = maxCost;
            return this;
        }

        public Builder firstScheduledTime(LocalDateTime firstScheduledTime) {
            this.firstScheduledTime = firstScheduledTime;
            return this;
        }

        public Builder lastScheduledTime(LocalDateTime lastScheduledTime) {
            this.lastScheduledTime = lastScheduledTime;
            return this;
        }

        public ActivitySummaryDTO build() {
            return new ActivitySummaryDTO(itineraryId, totalActivities, averageCost, maxCost, firstScheduledTime, lastScheduledTime);
        }
    }

    // Getters and Setters for all fields
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