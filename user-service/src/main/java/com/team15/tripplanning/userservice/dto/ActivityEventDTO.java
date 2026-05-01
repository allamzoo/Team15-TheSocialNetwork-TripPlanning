package com.team15.tripplanning.userservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ActivityEventDTO {
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    private ActivityEventDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ActivityEventDTO dto = new ActivityEventDTO();

        public Builder action(String action) {
            dto.action = action;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            dto.timestamp = timestamp;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            dto.details = details;
            return this;
        }

        public ActivityEventDTO build() {
            return dto;
        }
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
