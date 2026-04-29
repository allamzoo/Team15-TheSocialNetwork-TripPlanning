package com.team15.tripplanning.activityservice.model.mongo;

import com.team15.tripplanning.shared.mongo.MongoEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "activity_events")
public class ActivityEvent implements MongoEvent {

    @Id
    private String id;

    private Long activityId;
    private Long itineraryId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public ActivityEvent() {}

    public ActivityEvent(Map<String, Object> params) {
        this.activityId  = params.get("activityId")  instanceof Number n ? n.longValue() : null;
        this.itineraryId = params.get("itineraryId") instanceof Number n ? n.longValue() : null;
        this.action      = (String) params.getOrDefault("action", "ACTIVITY");
        this.timestamp   = LocalDateTime.now();
        this.details     = new HashMap<>(params);
    }

    @Override public String getId()                   { return id; }
    public void setId(String id)                      { this.id = id; }

    @Override public LocalDateTime getTimestamp()     { return timestamp; }
    @Override public String getAction()               { return action; }
    @Override public Map<String, Object> getDetails() { return details; }

    public Long getActivityId()  { return activityId; }
    public Long getItineraryId() { return itineraryId; }
}
