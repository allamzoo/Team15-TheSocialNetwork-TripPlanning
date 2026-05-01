package com.team15.tripplanning.itineraryservice.dto;

import java.util.HashMap;
import java.util.Map;

public class ItineraryAnalyticsDashboardDTO {

    private long totalItineraries;
    private double totalBudget;
    private double averageBudget;
    private double completionRate;
    private Map<String, Long> itinerariesByStatus;

    private ItineraryAnalyticsDashboardDTO() {}

    // --- Builder (same pattern as your other DTOs) ---
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ItineraryAnalyticsDashboardDTO dto = new ItineraryAnalyticsDashboardDTO();

        public Builder totalItineraries(long v) { dto.totalItineraries = v; return this; }
        public Builder totalBudget(double v) { dto.totalBudget = v; return this; }
        public Builder averageBudget(double v) { dto.averageBudget = v; return this; }
        public Builder completionRate(double v) { dto.completionRate = v; return this; }
        public Builder itinerariesByStatus(Map<String, Long> v) {
            dto.itinerariesByStatus = v != null ? v : new HashMap<>();
            return this;
        }

        public ItineraryAnalyticsDashboardDTO build() {
            return dto;
        }
    }

    // --- Getters / Setters (needed for JSON serialisation) ---
    public long getTotalItineraries() { return totalItineraries; }
    public void setTotalItineraries(long totalItineraries) { this.totalItineraries = totalItineraries; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getAverageBudget() { return averageBudget; }
    public void setAverageBudget(double averageBudget) { this.averageBudget = averageBudget; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public Map<String, Long> getItinerariesByStatus() { return itinerariesByStatus; }
    public void setItinerariesByStatus(Map<String, Long> itinerariesByStatus) {
        this.itinerariesByStatus = itinerariesByStatus;
    }
}