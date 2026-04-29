package com.team15.tripplanning.shared.mongo;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class PaymentAuditEvent implements MongoEvent {
    private final String id;
    private final LocalDateTime timestamp;
    private final String action;
    private final Map<String, Object> details;

    public PaymentAuditEvent(Map<String, Object> params) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.action = (String) params.getOrDefault("action", "PAYMENT_AUDIT");
        this.details = params;
    }

    @Override public String getId() { return id; }
    @Override public LocalDateTime getTimestamp() { return timestamp; }
    @Override public String getAction() { return action; }
    @Override public Map<String, Object> getDetails() { return details; }
}
