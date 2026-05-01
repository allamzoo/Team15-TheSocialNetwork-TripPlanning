package com.team15.tripplanning.activityservice.dto;

import java.time.Instant;

public class ActivityLifecycleEventDTO {

    private Long activityId;
    private Instant eventTimestamp;
    private String status;
    private String category;
    private Double latitude;
    private Double longitude;
    private String notes;

    public ActivityLifecycleEventDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long activityId;
        private Instant eventTimestamp;
        private String status;
        private String category;
        private Double latitude;
        private Double longitude;
        private String notes;

        public Builder activityId(Long v)        { activityId = v;       return this; }
        public Builder eventTimestamp(Instant v) { eventTimestamp = v;   return this; }
        public Builder status(String v)          { status = v;           return this; }
        public Builder category(String v)        { category = v;         return this; }
        public Builder latitude(Double v)        { latitude = v;         return this; }
        public Builder longitude(Double v)       { longitude = v;        return this; }
        public Builder notes(String v)           { notes = v;            return this; }

        public ActivityLifecycleEventDTO build() {
            ActivityLifecycleEventDTO dto = new ActivityLifecycleEventDTO();
            dto.activityId      = activityId;
            dto.eventTimestamp  = eventTimestamp;
            dto.status          = status;
            dto.category        = category;
            dto.latitude        = latitude;
            dto.longitude       = longitude;
            dto.notes           = notes;
            return dto;
        }
    }

    public Long getActivityId()          { return activityId; }
    public Instant getEventTimestamp()   { return eventTimestamp; }
    public String getStatus()            { return status; }
    public String getCategory()          { return category; }
    public Double getLatitude()          { return latitude; }
    public Double getLongitude()         { return longitude; }
    public String getNotes()             { return notes; }
}
