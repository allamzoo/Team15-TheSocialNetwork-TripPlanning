package com.team15.tripplanning.bookingservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class PaymentAuditEventDTO {
    private String action;
    private LocalDateTime timestamp;
    private Double amount;
    private String method;
    private Map<String, Object> details;
}