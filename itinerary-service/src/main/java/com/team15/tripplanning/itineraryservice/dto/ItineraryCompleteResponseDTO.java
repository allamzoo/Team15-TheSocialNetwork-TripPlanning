package com.team15.tripplanning.itineraryservice.dto;

public class ItineraryCompleteResponseDTO {
    private Long id;
    private Long userId;
    private Long destinationId;
    private String title;
    private String status;
    private Double estimatedBudget;

    public ItineraryCompleteResponseDTO() {}

    public ItineraryCompleteResponseDTO(Long id, Long userId, Long destinationId,
                                        String title, String status, Double estimatedBudget) {
        this.id = id;
        this.userId = userId;
        this.destinationId = destinationId;
        this.title = title;
        this.status = status;
        this.estimatedBudget = estimatedBudget;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getEstimatedBudget() { return estimatedBudget; }
    public void setEstimatedBudget(Double estimatedBudget) { this.estimatedBudget = estimatedBudget; }
}