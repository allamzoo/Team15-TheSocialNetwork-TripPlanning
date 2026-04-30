package com.team15.tripplanning.itineraryservice.dto;

public class ItineraryAnalyticsDTO {
    private Long totalItineraries;
    private Long completedItineraries;
    private Long cancelledItineraries;
    private Double totalBudget;
    private Double averageBudget;
    private Double completionRate;

    private ItineraryAnalyticsDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ItineraryAnalyticsDTO dto = new ItineraryAnalyticsDTO();

        public Builder totalItineraries(Long v) {
            dto.totalItineraries = v;
            return this;
        }

        public Builder completedItineraries(Long v) {
            dto.completedItineraries = v;
            return this;
        }

        public Builder cancelledItineraries(Long v) {
            dto.cancelledItineraries = v;
            return this;
        }

        public Builder totalBudget(Double v) {
            dto.totalBudget = v;
            return this;
        }

        public Builder averageBudget(Double v) {
            dto.averageBudget = v;
            return this;
        }

        public Builder completionRate(Double v) {
            dto.completionRate = v;
            return this;
        }

        public ItineraryAnalyticsDTO build() {
            return dto;
        }
    }

    public Long getTotalItineraries() { return totalItineraries; }
    public void setTotalItineraries(Long v) { this.totalItineraries = v; }

    public Long getCompletedItineraries() { return completedItineraries; }
    public void setCompletedItineraries(Long v) { this.completedItineraries = v; }

    public Long getCancelledItineraries() { return cancelledItineraries; }
    public void setCancelledItineraries(Long v) { this.cancelledItineraries = v; }

    public Double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(Double v) { this.totalBudget = v; }

    public Double getAverageBudget() { return averageBudget; }
    public void setAverageBudget(Double v) { this.averageBudget = v; }

    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double v) { this.completionRate = v; }
}
