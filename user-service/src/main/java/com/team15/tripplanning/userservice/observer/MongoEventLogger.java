package com.team15.tripplanning.userservice.observer;

import com.team15.tripplanning.userservice.document.AuthEvent;
import com.team15.tripplanning.userservice.repository.AuthEventRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final AuthEventRepository eventRepository;
    private final EventType boundEventType = EventType.AUTH;

    public MongoEventLogger(AuthEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", eventType);
        if (payload instanceof Map) {
            params.putAll((Map<String, Object>) payload);
        }
        CompletableFuture.runAsync(() -> {
            try {
                MongoEvent event = EventFactory.createEvent(boundEventType, params);
                eventRepository.save((AuthEvent) event);
            } catch (Exception e) {
                log.warn("MongoDB event logging failed: {}", e.getMessage());
            }
        });
    }
}
