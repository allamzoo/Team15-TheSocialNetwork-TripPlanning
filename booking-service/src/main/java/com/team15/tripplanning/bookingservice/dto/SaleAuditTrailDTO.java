package com.team15.tripplanning.bookingservice.dto;

import java.util.List;

public class SaleAuditTrailDTO {
    private Long saleId;
    private List<AuditEventDTO> events;

    private SaleAuditTrailDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SaleAuditTrailDTO obj = new SaleAuditTrailDTO();

        public Builder saleId(Long v)                   { obj.saleId = v; return this; }
        public Builder events(List<AuditEventDTO> v)    { obj.events = v; return this; }

        public SaleAuditTrailDTO build() { return obj; }
    }

    public Long getSaleId()                 { return saleId; }
    public List<AuditEventDTO> getEvents()  { return events; }
}
