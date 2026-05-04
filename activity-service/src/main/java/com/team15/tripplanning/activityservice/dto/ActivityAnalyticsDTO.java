package com.team15.tripplanning.activityservice.dto;

import java.util.Map;

public class ActivityAnalyticsDTO {

    private int totalActivities;
    private double averageCost;
    private double averageDurationHours;
    private Map<String, Long> activitiesByCategory;

    private ActivityAnalyticsDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ActivityAnalyticsDTO dto = new ActivityAnalyticsDTO();

        public Builder totalActivities(int totalActivities) {
            dto.totalActivities = totalActivities;
            return this;
        }

        public Builder averageCost(double averageCost) {
            dto.averageCost = averageCost;
            return this;
        }

        public Builder averageDurationHours(double averageDurationHours) {
            dto.averageDurationHours = averageDurationHours;
            return this;
        }

        public Builder activitiesByCategory(Map<String, Long> activitiesByCategory) {
            dto.activitiesByCategory = activitiesByCategory;
            return this;
        }

        public ActivityAnalyticsDTO build() {
            return dto;
        }
    }

    public int getTotalActivities() { return totalActivities; }
    public void setTotalActivities(int totalActivities) { this.totalActivities = totalActivities; }

    public double getAverageCost() { return averageCost; }
    public void setAverageCost(double averageCost) { this.averageCost = averageCost; }

    public double getAverageDurationHours() { return averageDurationHours; }
    public void setAverageDurationHours(double averageDurationHours) { this.averageDurationHours = averageDurationHours; }

    public Map<String, Long> getActivitiesByCategory() { return activitiesByCategory; }
    public void setActivitiesByCategory(Map<String, Long> activitiesByCategory) { this.activitiesByCategory = activitiesByCategory; }
}
