package com.team15.tripplanning.userservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ActivityFeedDTO {
    private String action;
    private LocalDateTime timestamp;
    private Long userId;
    private Map<String, Object> details;

    private ActivityFeedDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ActivityFeedDTO dto = new ActivityFeedDTO();

        public Builder action(String action) {
            dto.action = action;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            dto.timestamp = timestamp;
            return this;
        }

        public Builder userId(Long userId) {
            dto.userId = userId;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            dto.details = details;
            return this;
        }

        public ActivityFeedDTO build() {
            return dto;
        }
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
