package com.team15.tripplanning.itineraryservice.dto;

public class ItineraryAnalyticsDTO {
    private Long totalItineraries;
    private Long completedItineraries;
    private Long cancelledItineraries;
    private Double totalBudget;
    private Double averageBudget;
    private Double completionRate;

    public ItineraryAnalyticsDTO() {}

    public ItineraryAnalyticsDTO(Long totalItineraries, Long completedItineraries,
                                 Long cancelledItineraries, Double totalBudget,
                                 Double averageBudget, Double completionRate) {
        this.totalItineraries = totalItineraries;
        this.completedItineraries = completedItineraries;
        this.cancelledItineraries = cancelledItineraries;
        this.totalBudget = totalBudget;
        this.averageBudget = averageBudget;
        this.completionRate = completionRate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long totalItineraries;
        private Long completedItineraries;
        private Long cancelledItineraries;
        private Double totalBudget;
        private Double averageBudget;
        private Double completionRate;

        public Builder totalItineraries(Long totalItineraries) {
            this.totalItineraries = totalItineraries;
            return this;
        }

        public Builder completedItineraries(Long completedItineraries) {
            this.completedItineraries = completedItineraries;
            return this;
        }

        public Builder cancelledItineraries(Long cancelledItineraries) {
            this.cancelledItineraries = cancelledItineraries;
            return this;
        }

        public Builder totalBudget(Double totalBudget) {
            this.totalBudget = totalBudget;
            return this;
        }

        public Builder averageBudget(Double averageBudget) {
            this.averageBudget = averageBudget;
            return this;
        }

        public Builder completionRate(Double completionRate) {
            this.completionRate = completionRate;
            return this;
        }

        public ItineraryAnalyticsDTO build() {
            return new ItineraryAnalyticsDTO(totalItineraries, completedItineraries, cancelledItineraries, totalBudget, averageBudget, completionRate);
        }
    }

    public Long getTotalItineraries() { return totalItineraries; }
    public void setTotalItineraries(Long totalItineraries) { this.totalItineraries = totalItineraries; }

    public Long getCompletedItineraries() { return completedItineraries; }
    public void setCompletedItineraries(Long completedItineraries) { this.completedItineraries = completedItineraries; }

    public Long getCancelledItineraries() { return cancelledItineraries; }
    public void setCancelledItineraries(Long cancelledItineraries) { this.cancelledItineraries = cancelledItineraries; }

    public Double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(Double totalBudget) { this.totalBudget = totalBudget; }

    public Double getAverageBudget() { return averageBudget; }
    public void setAverageBudget(Double averageBudget) { this.averageBudget = averageBudget; }

    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
}