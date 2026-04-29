package com.team15.tripplanning.bookingservice.model.mongo;

import com.team15.tripplanning.shared.mongo.MongoEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "payment_audit_trail")
public class PaymentAuditEvent implements MongoEvent {

    @Id
    private String id;

    private Long bookingId;
    private Long userId;
    private String action;
    private String method;
    private Double amount;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public PaymentAuditEvent() {}

    public PaymentAuditEvent(Map<String, Object> params) {
        this.bookingId = params.get("bookingId") instanceof Number n ? n.longValue() : null;
        this.userId    = params.get("userId")    instanceof Number n ? n.longValue() : null;
        this.action    = (String) params.getOrDefault("action", "PAYMENT_AUDIT");
        this.method    = (String) params.get("method");
        this.amount    = params.get("amount") instanceof Number n ? n.doubleValue() : null;
        this.timestamp = LocalDateTime.now();
        this.details   = new HashMap<>(params);
    }

    @Override public String getId()                   { return id; }
    public void setId(String id)                      { this.id = id; }

    @Override public LocalDateTime getTimestamp()     { return timestamp; }
    @Override public String getAction()               { return action; }
    @Override public Map<String, Object> getDetails() { return details; }

    public Long getBookingId() { return bookingId; }
    public Long getUserId()    { return userId; }
    public String getMethod()  { return method; }
    public Double getAmount()  { return amount; }
}
