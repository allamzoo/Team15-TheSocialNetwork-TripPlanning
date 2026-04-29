package com.team15.tripplanning.userservice.document;

import com.team15.tripplanning.userservice.observer.MongoEvent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auth_events")
public class AuthEvent implements MongoEvent {

    @Id
    private String id;
    private Long userId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public AuthEvent(Map<String, Object> params) {
        this.userId = (Long) params.get("userId");
        this.action = (String) params.get("action");
        this.timestamp = LocalDateTime.now();
        this.details = new HashMap<>(params);
    }

    @Override public String getId() { return id; }
    @Override public LocalDateTime getTimestamp() { return timestamp; }
    @Override public String getAction() { return action; }
    @Override public Map<String, Object> getDetails() { return details; }

    public Long getUserId() { return userId; }

    public void setId(String id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setAction(String action) { this.action = action; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
