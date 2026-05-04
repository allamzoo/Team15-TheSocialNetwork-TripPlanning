package com.team15.tripplanning.activityservice.dto;

import java.io.Serializable;
import java.time.Instant;

public class ActivityEventDTO implements Serializable {

    private Instant timestamp;
    private String status;
    private String category;
    private Double latitude;
    private Double longitude;
    private String notes;

    public ActivityEventDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Instant timestamp;
        private String status;
        private String category;
        private Double latitude;
        private Double longitude;
        private String notes;

        public Builder timestamp(Instant v)  { timestamp = v;  return this; }
        public Builder status(String v)      { status = v;     return this; }
        public Builder category(String v)    { category = v;   return this; }
        public Builder latitude(Double v)    { latitude = v;   return this; }
        public Builder longitude(Double v)   { longitude = v;  return this; }
        public Builder notes(String v)       { notes = v;      return this; }

        public ActivityEventDTO build() {
            ActivityEventDTO dto = new ActivityEventDTO();
            dto.timestamp  = timestamp;
            dto.status     = status;
            dto.category   = category;
            dto.latitude   = latitude;
            dto.longitude  = longitude;
            dto.notes      = notes;
            return dto;
        }
    }

    public Instant getTimestamp()  { return timestamp; }
    public String getStatus()      { return status; }
    public String getCategory()    { return category; }
    public Double getLatitude()    { return latitude; }
    public Double getLongitude()   { return longitude; }
    public String getNotes()       { return notes; }
}
