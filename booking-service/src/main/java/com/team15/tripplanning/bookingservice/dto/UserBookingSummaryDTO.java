package com.team15.tripplanning.bookingservice.dto;

import java.util.Map;

public class UserBookingSummaryDTO {

    private Long userId;
    private int totalBookings;
    private double totalAmount;
    private Map<String, Double> typeBreakdown;

    public UserBookingSummaryDTO(Long userId, int totalBookings, double totalAmount, Map<String, Double> typeBreakdown) {
        this.userId = userId;
        this.totalBookings = totalBookings;
        this.totalAmount = totalAmount;
        this.typeBreakdown = typeBreakdown;
    }

    public Long getUserId() {
        return userId;
    }

    public int getTotalBookings() {
        return totalBookings;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Map<String, Double> getTypeBreakdown() {
        return typeBreakdown;
    }
}
