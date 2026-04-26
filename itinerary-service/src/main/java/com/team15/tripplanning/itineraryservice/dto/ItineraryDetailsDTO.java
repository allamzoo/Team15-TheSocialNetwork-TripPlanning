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

    // ✅ Default constructor
    public ItineraryDetailsDTO() {
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