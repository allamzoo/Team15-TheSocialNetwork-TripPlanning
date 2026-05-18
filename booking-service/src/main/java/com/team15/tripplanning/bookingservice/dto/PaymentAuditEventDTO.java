package com.team15.tripplanning.bookingservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class PaymentAuditEventDTO {

    private String action;
    private String eventType;
    private LocalDateTime timestamp;
    private Double amount;
    private String method;
    private Map<String, Object> details;

    private PaymentAuditEventDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final PaymentAuditEventDTO dto = new PaymentAuditEventDTO();

        public Builder action(String v)                  { dto.action = v;     return this; }
        public Builder eventType(String v)               { dto.eventType = v;  return this; }
        public Builder timestamp(LocalDateTime v)         { dto.timestamp = v;  return this; }
        public Builder amount(Double v)                  { dto.amount = v;     return this; }
        public Builder method(String v)                  { dto.method = v;     return this; }
        public Builder details(Map<String, Object> v)    { dto.details = v;    return this; }

        public PaymentAuditEventDTO build() { return dto; }
    }

    public String getAction()                { return action; }
    public void setAction(String action)     { this.action = action; }

    public String getEventType()             { return eventType; }
    public void setEventType(String v)       { this.eventType = v; }

    public LocalDateTime getTimestamp()                   { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp)     { this.timestamp = timestamp; }

    public Double getAmount()                { return amount; }
    public void setAmount(Double amount)     { this.amount = amount; }

    public String getMethod()                { return method; }
    public void setMethod(String method)     { this.method = method; }

    public Map<String, Object> getDetails()              { return details; }
    public void setDetails(Map<String, Object> details)  { this.details = details; }
}
