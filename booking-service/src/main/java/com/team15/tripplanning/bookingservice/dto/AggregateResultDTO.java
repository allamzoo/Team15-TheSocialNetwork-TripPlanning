package com.team15.tripplanning.bookingservice.dto;

import java.math.BigDecimal;

/** Response for aggregate booking queries (count + total amount). */
public class AggregateResultDTO {

    private long bookingCount;
    private BigDecimal totalAmount;

    public AggregateResultDTO() {}

    public AggregateResultDTO(long bookingCount, BigDecimal totalAmount) {
        this.bookingCount = bookingCount;
        this.totalAmount = totalAmount;
    }

    public long getBookingCount() { return bookingCount; }
    public void setBookingCount(long bookingCount) { this.bookingCount = bookingCount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
