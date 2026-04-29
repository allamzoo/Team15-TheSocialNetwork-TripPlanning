package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.model.mongo.DestinationEvent;
import com.team15.tripplanning.destinationservice.repository.DestinationEventRepository;
import com.team15.tripplanning.shared.event.EventFactory;
import com.team15.tripplanning.shared.event.EventType;
import com.team15.tripplanning.shared.mongo.MongoEvent;
import com.team15.tripplanning.shared.observer.EntityObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final DestinationEventRepository eventRepository;
    private final EventType boundEventType = EventType.DESTINATION;

    public MongoEventLogger(DestinationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("action", eventType);

            if (payload instanceof Map<?, ?> map) {
                map.forEach((k, v) -> params.put(k.toString(), v));
            }

            // Use Factory to create the event (GoF Factory pattern)
            MongoEvent event = EventFactory.createEvent(boundEventType, params);

            // Build the @Document entity from the factory event's details and save
            DestinationEvent doc = new DestinationEvent(event.getDetails());
            eventRepository.save(doc);

        } catch (Exception e) {
            // Soft dependency — never rethrow; logging must not break business logic
            log.warn("MongoDB event logging failed: {}", e.getMessage());
        }
    }
}
