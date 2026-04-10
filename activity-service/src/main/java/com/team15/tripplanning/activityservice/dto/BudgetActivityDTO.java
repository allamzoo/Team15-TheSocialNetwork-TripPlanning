package com.team15.tripplanning.activityservice.dto;
import java.time.LocalDateTime;

public class BudgetActivityDTO {
    private Long activityId;
    private String name;
    private String category;
    private Double latitude;
    private Double longitude;
    private Double cost;
    private LocalDateTime scheduledTime;

    public BudgetActivityDTO(Long activityId, String name, String category,
                             Double latitude, Double longitude,
                             Double cost, LocalDateTime scheduledTime) {
        this.activityId = activityId;
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cost = cost;
        this.scheduledTime = scheduledTime;
    }

    // Getters and Setters
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
}