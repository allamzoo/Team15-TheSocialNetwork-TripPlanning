package com.team15.tripplanning.activityservice.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("activity_lifecycle_events")
public class ActivityLifecycleEvent {

    @PrimaryKey
    private ActivityLifecycleEventKey key;

    @Column("status")
    private String status;

    @Column("category")
    private String category;

    @Column("latitude")
    private Double latitude;

    @Column("longitude")
    private Double longitude;

    @Column("notes")
    private String notes;

    public ActivityLifecycleEvent() {}

    public ActivityLifecycleEventKey getKey() { return key; }
    public void setKey(ActivityLifecycleEventKey key) { this.key = key; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
