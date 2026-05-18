package com.team15.tripplanning.contracts.dto;

import java.math.BigDecimal;

/**
 * Returned by booking-service GET /api/bookings/user/{userId}/total?startDate=&endDate=
 * (called by user-service S1-F6 to rank travelers by spending).
 * totalAmount = sum of CONFIRMED bookings for this user in the date range.
 */
public record UserBookingTotalDTO(
        Long userId,
        BigDecimal totalAmount,
        long tripCount
) {}
