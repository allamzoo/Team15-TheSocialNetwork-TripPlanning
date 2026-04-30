package com.team15.tripplanning.itineraryservice.dto;

import java.util.List;

public class ItineraryDetailsDTO {
    private Long itineraryId;
    private Long userId;
    private Long destinationId;
    private String title;
    private String status;
    private Double estimatedBudget;
    private Object metadata;
    private List<ItineraryDayDTO> days;
    private int totalDays;
    private int completedDays;

    private ItineraryDetailsDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ItineraryDetailsDTO dto = new ItineraryDetailsDTO();

        public Builder itineraryId(Long v) {
            dto.itineraryId = v;
            return this;
        }

        public Builder userId(Long v) {
            dto.userId = v;
            return this;
        }

        public Builder destinationId(Long v) {
            dto.destinationId = v;
            return this;
        }

        public Builder title(String v) {
            dto.title = v;
            return this;
        }

        public Builder status(String v) {
            dto.status = v;
            return this;
        }

        public Builder estimatedBudget(Double v) {
            dto.estimatedBudget = v;
            return this;
        }

        public Builder metadata(Object v) {
            dto.metadata = v;
            return this;
        }

        public Builder days(List<ItineraryDayDTO> v) {
            dto.days = v;
            return this;
        }

        public Builder totalDays(int v) {
            dto.totalDays = v;
            return this;
        }

        public Builder completedDays(int v) {
            dto.completedDays = v;
            return this;
        }

        public ItineraryDetailsDTO build() {
            return dto;
        }
    }

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long v) { this.itineraryId = v; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long v) { this.destinationId = v; }

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Double getEstimatedBudget() { return estimatedBudget; }
    public void setEstimatedBudget(Double v) { this.estimatedBudget = v; }

    public Object getMetadata() { return metadata; }
    public void setMetadata(Object v) { this.metadata = v; }

    public List<ItineraryDayDTO> getDays() { return days; }
    public void setDays(List<ItineraryDayDTO> v) { this.days = v; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int v) { this.totalDays = v; }

    public int getCompletedDays() { return completedDays; }
    public void setCompletedDays(int v) { this.completedDays = v; }
}
