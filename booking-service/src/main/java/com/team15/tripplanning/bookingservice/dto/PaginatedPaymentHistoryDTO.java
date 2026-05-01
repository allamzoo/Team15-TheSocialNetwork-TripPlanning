package com.team15.tripplanning.bookingservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PaginatedPaymentHistoryDTO {
    private List<PaymentAuditEventDTO> content;
    private int page;
    private int size;
    private long totalElements;
}
