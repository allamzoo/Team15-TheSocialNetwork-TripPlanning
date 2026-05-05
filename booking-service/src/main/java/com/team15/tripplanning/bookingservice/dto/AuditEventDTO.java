package com.team15.tripplanning.bookingservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class AuditEventDTO {
    private String action;
    private LocalDateTime timestamp;
    private String method;
    private Double amount;
    private Map<String, Object> details;

    private AuditEventDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AuditEventDTO obj = new AuditEventDTO();

        public Builder action(String v)                { obj.action = v; return this; }
        public Builder timestamp(LocalDateTime v)      { obj.timestamp = v; return this; }
        public Builder method(String v)                { obj.method = v; return this; }
        public Builder amount(Double v)                { obj.amount = v; return this; }
        public Builder details(Map<String, Object> v)  { obj.details = v; return this; }

        public AuditEventDTO build() { return obj; }
    }

    public String getAction()               { return action; }
    public LocalDateTime getTimestamp()     { return timestamp; }
    public String getMethod()               { return method; }
    public Double getAmount()               { return amount; }
    public Map<String, Object> getDetails() { return details; }
}
