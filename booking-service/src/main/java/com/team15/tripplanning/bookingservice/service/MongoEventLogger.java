package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.mongo.PaymentAuditEvent;
import com.team15.tripplanning.bookingservice.repository.PaymentAuditEventRepository;
import com.team15.tripplanning.shared.event.EventFactory;
import com.team15.tripplanning.shared.event.EventType;
import com.team15.tripplanning.shared.mongo.MongoEvent;
import com.team15.tripplanning.shared.observer.EntityObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final PaymentAuditEventRepository eventRepository;
    private final EventType boundEventType = EventType.PAYMENT_AUDIT;

    public MongoEventLogger(PaymentAuditEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", eventType);
        if (payload instanceof Map<?, ?> map) {
            map.forEach((k, v) -> params.put(k.toString(), v));
        }
        CompletableFuture.runAsync(() -> {
            try {
                // Use Factory to create the event (GoF Factory pattern)
                MongoEvent event = EventFactory.createEvent(boundEventType, params);
                // Build the @Document entity from the factory event's details and save
                PaymentAuditEvent doc = new PaymentAuditEvent(event.getDetails());
                eventRepository.save(doc);
            } catch (Exception e) {
                // Soft dependency — never rethrow; logging must not break business logic
                log.warn("MongoDB event logging failed: {}", e.getMessage());
            }
        });
    }
}
