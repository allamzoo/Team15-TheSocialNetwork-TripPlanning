package com.team15.tripplanning.destinationservice.model.mongo;

import com.team15.tripplanning.shared.mongo.MongoEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "destination_events")
public class DestinationEvent implements MongoEvent {

    @Id
    private String id;

    private Long destinationId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public DestinationEvent() {}

    public DestinationEvent(Map<String, Object> params) {
        this.destinationId = params.get("destinationId") instanceof Number n ? n.longValue() : null;
        this.action        = (String) params.getOrDefault("action", "DESTINATION");
        this.timestamp     = LocalDateTime.now();
        this.details       = new HashMap<>(params);
    }

    @Override public String getId()                   { return id; }
    public void setId(String id)                      { this.id = id; }

    @Override public LocalDateTime getTimestamp()     { return timestamp; }
    @Override public String getAction()               { return action; }
    @Override public Map<String, Object> getDetails() { return details; }

    public Long getDestinationId() { return destinationId; }
}
