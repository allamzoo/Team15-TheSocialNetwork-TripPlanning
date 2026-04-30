package com.team15.tripplanning.activityservice.model.cassandra;

import java.time.LocalDateTime;

public class ActivityLogRow {
    private Long activityId;
    private String name;
    private String category;
    private Double latitude;
    private Double longitude;
    private Double cost;
    private Long itineraryId;
    private LocalDateTime scheduledTime;

    public ActivityLogRow() {}

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

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
}
