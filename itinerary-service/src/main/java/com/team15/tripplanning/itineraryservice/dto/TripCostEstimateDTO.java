package com.team15.tripplanning.itineraryservice.dto;

public class TripCostEstimateDTO {
    private double estimatedAccommodation;
    private double estimatedTransport;
    private double estimatedActivities;
    private double estimatedTotal;
    private double seasonMultiplier;

    private TripCostEstimateDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TripCostEstimateDTO dto = new TripCostEstimateDTO();

        public Builder estimatedAccommodation(double estimatedAccommodation) {
            dto.estimatedAccommodation = estimatedAccommodation;
            return this;
        }

        public Builder estimatedTransport(double estimatedTransport) {
            dto.estimatedTransport = estimatedTransport;
            return this;
        }

        public Builder estimatedActivities(double estimatedActivities) {
            dto.estimatedActivities = estimatedActivities;
            return this;
        }

        public Builder estimatedTotal(double estimatedTotal) {
            dto.estimatedTotal = estimatedTotal;
            return this;
        }

        public Builder seasonMultiplier(double seasonMultiplier) {
            dto.seasonMultiplier = seasonMultiplier;
            return this;
        }

        public TripCostEstimateDTO build() {
            return dto;
        }
    }

    public TripCostEstimateDTO(double estimatedAccommodation, double estimatedTransport,
                               double estimatedActivities, double estimatedTotal,
                               double seasonMultiplier) {
        this.estimatedAccommodation = estimatedAccommodation;
        this.estimatedTransport = estimatedTransport;
        this.estimatedActivities = estimatedActivities;
        this.estimatedTotal = estimatedTotal;
        this.seasonMultiplier = seasonMultiplier;
    }

    public double getEstimatedAccommodation() { return estimatedAccommodation; }
    public void setEstimatedAccommodation(double v) { this.estimatedAccommodation = v; }

    public double getEstimatedTransport() { return estimatedTransport; }
    public void setEstimatedTransport(double v) { this.estimatedTransport = v; }

    public double getEstimatedActivities() { return estimatedActivities; }
    public void setEstimatedActivities(double v) { this.estimatedActivities = v; }

    public double getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(double v) { this.estimatedTotal = v; }

    public double getSeasonMultiplier() { return seasonMultiplier; }
    public void setSeasonMultiplier(double v) { this.seasonMultiplier = v; }
}