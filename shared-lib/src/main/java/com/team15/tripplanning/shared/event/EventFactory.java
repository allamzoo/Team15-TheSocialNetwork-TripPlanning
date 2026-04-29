package com.team15.tripplanning.shared.event;

import com.team15.tripplanning.shared.mongo.*;

import java.util.Map;

public class EventFactory {

    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        return switch (type) {
            case AUTH          -> new AuthEvent(params);
            case DESTINATION   -> new DestinationEvent(params);
            case ITINERARY     -> new ItineraryEvent(params);
            case ACTIVITY      -> new ActivityEvent(params);
            case PAYMENT_AUDIT -> new PaymentAuditEvent(params);
        };
    }
}
