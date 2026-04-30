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

        public Builder itineraryId(Long itineraryId) {
            dto.itineraryId = itineraryId;
            return this;
        }

        public Builder userId(Long userId) {
            dto.userId = userId;
            return this;
        }

        public Builder destinationId(Long destinationId) {
            dto.destinationId = destinationId;
            return this;
        }

        public Builder title(String title) {
            dto.title = title;
            return this;
        }

        public Builder status(String status) {
            dto.status = status;
            return this;
        }

        public Builder estimatedBudget(Double estimatedBudget) {
            dto.estimatedBudget = estimatedBudget;
            return this;
        }

        public Builder metadata(Object metadata) {
            dto.metadata = metadata;
            return this;
        }

        public Builder days(List<ItineraryDayDTO> days) {
            dto.days = days;
            return this;
        }

        public Builder totalDays(int totalDays) {
            dto.totalDays = totalDays;
            return this;
        }

        public Builder completedDays(int completedDays) {
            dto.completedDays = completedDays;
            return this;
        }

        public ItineraryDetailsDTO build() {
            return dto;
        }
    }

    // ✅ Full constructor
    public ItineraryDetailsDTO(Long itineraryId, Long userId, Long destinationId,
                               String title, String status, Double estimatedBudget,
                               Object metadata, List<ItineraryDayDTO> days,
                               int totalDays, int completedDays) {
        this.itineraryId = itineraryId;
        this.userId = userId;
        this.destinationId = destinationId;
        this.title = title;
        this.status = status;
        this.estimatedBudget = estimatedBudget;
        this.metadata = metadata;
        this.days = days;
        this.totalDays = totalDays;
        this.completedDays = completedDays;
    }

    // ✅ Getters and Setters

    public Long getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(Long itineraryId) {
        this.itineraryId = itineraryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getEstimatedBudget() {
        return estimatedBudget;
    }

    public void setEstimatedBudget(Double estimatedBudget) {
        this.estimatedBudget = estimatedBudget;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public List<ItineraryDayDTO> getDays() {
        return days;
    }

    public void setDays(List<ItineraryDayDTO> days) {
        this.days = days;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getCompletedDays() {
        return completedDays;
    }

    public void setCompletedDays(int completedDays) {
        this.completedDays = completedDays;
    }
}