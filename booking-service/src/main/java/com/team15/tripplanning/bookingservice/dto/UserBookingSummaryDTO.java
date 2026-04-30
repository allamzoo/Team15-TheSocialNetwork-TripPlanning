package com.team15.tripplanning.bookingservice.dto;

import java.util.Map;

public class UserBookingSummaryDTO {
    private Long userId;
    private int totalBookings;
    private double totalAmount;
    private Map<String, Double> typeBreakdown;

    private UserBookingSummaryDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UserBookingSummaryDTO dto = new UserBookingSummaryDTO();

        public Builder userId(Long userId) { dto.userId = userId; return this; }
        public Builder totalBookings(int totalBookings) { dto.totalBookings = totalBookings; return this; }
        public Builder totalAmount(double totalAmount) { dto.totalAmount = totalAmount; return this; }
        public Builder typeBreakdown(Map<String, Double> typeBreakdown) { dto.typeBreakdown = typeBreakdown; return this; }

        public UserBookingSummaryDTO build() { return dto; }
    }

    public Long getUserId() { return userId; }
    public int getTotalBookings() { return totalBookings; }
    public double getTotalAmount() { return totalAmount; }
    public Map<String, Double> getTypeBreakdown() { return typeBreakdown; }
}
