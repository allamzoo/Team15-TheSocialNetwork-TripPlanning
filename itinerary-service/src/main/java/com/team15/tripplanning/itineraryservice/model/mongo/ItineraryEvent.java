package com.team15.tripplanning.itineraryservice.model.mongo;

import com.team15.tripplanning.shared.mongo.MongoEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "itinerary_events")
public class ItineraryEvent implements MongoEvent {

    @Id
    private String id;

    private Long itineraryId;
    private Long userId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public ItineraryEvent() {}

    public ItineraryEvent(Map<String, Object> params) {
        this.itineraryId = params.get("itineraryId") instanceof Number n ? n.longValue() : null;
        this.userId      = params.get("userId")      instanceof Number n ? n.longValue() : null;
        this.action      = (String) params.getOrDefault("action", "ITINERARY");
        this.timestamp   = LocalDateTime.now();
        this.details     = new HashMap<>(params);
    }

    @Override public String getId()                   { return id; }
    public void setId(String id)                      { this.id = id; }

    @Override public LocalDateTime getTimestamp()     { return timestamp; }
    @Override public String getAction()               { return action; }
    @Override public Map<String, Object> getDetails() { return details; }

    public Long getItineraryId() { return itineraryId; }
    public Long getUserId()      { return userId; }
}
