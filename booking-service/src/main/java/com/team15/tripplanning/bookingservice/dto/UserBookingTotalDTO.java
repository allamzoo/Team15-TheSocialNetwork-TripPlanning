package com.team15.tripplanning.bookingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Response for GET /api/bookings/user/{userId}/total */
public class UserBookingTotalDTO {

    private Long userId;
    private BigDecimal totalAmount;
    private long bookingCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public UserBookingTotalDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public long getBookingCount() { return bookingCount; }
    public void setBookingCount(long bookingCount) { this.bookingCount = bookingCount; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}
