package com.team15.tripplanning.bookingservice.dto;

import java.util.List;

public class PaginatedPaymentHistoryDTO {

    private List<PaymentAuditEventDTO> content;
    private int page;
    private int size;
    private long totalElements;

    private PaginatedPaymentHistoryDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final PaginatedPaymentHistoryDTO dto = new PaginatedPaymentHistoryDTO();

        public Builder content(List<PaymentAuditEventDTO> v) { dto.content = v;        return this; }
        public Builder page(int v)                           { dto.page = v;           return this; }
        public Builder size(int v)                           { dto.size = v;           return this; }
        public Builder totalElements(long v)                 { dto.totalElements = v;  return this; }

        public PaginatedPaymentHistoryDTO build() { return dto; }
    }

    public List<PaymentAuditEventDTO> getContent()               { return content; }
    public void setContent(List<PaymentAuditEventDTO> content)   { this.content = content; }

    public int getPage()         { return page; }
    public void setPage(int page){ this.page = page; }

    public int getSize()         { return size; }
    public void setSize(int size){ this.size = size; }

    public long getTotalElements()              { return totalElements; }
    public void setTotalElements(long total)    { this.totalElements = total; }
}
