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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private int totalBookings;
        private double totalAmount;
        private Map<String, Double> typeBreakdown;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder totalBookings(int totalBookings) {
            this.totalBookings = totalBookings;
            return this;
        }

        public Builder totalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder typeBreakdown(Map<String, Double> typeBreakdown) {
            this.typeBreakdown = typeBreakdown;
            return this;
        }

        public UserBookingSummaryDTO build() {
            return new UserBookingSummaryDTO(userId, totalBookings, totalAmount, typeBreakdown);
        }
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
