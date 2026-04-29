package com.team15.tripplanning.userservice.observer;

import com.team15.tripplanning.userservice.document.AuthEvent;
import java.util.Map;

public class EventFactory {

    private EventFactory() {}

    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        return switch (type) {
            case AUTH -> new AuthEvent(params);
            default -> throw new IllegalArgumentException("Unsupported event type: " + type);
        };
    }
}
