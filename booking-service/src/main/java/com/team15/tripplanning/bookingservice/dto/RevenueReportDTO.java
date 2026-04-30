package com.team15.tripplanning.bookingservice.dto;

public class RevenueReportDTO {
    private double totalRevenue;
    private long totalBookings;
    private double averageBookingAmount;
    private double cancelledAmount;
    private long cancelledCount;

    private RevenueReportDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RevenueReportDTO dto = new RevenueReportDTO();

        public Builder totalRevenue(double totalRevenue) { dto.totalRevenue = totalRevenue; return this; }
        public Builder totalBookings(long totalBookings) { dto.totalBookings = totalBookings; return this; }
        public Builder averageBookingAmount(double averageBookingAmount) { dto.averageBookingAmount = averageBookingAmount; return this; }
        public Builder cancelledAmount(double cancelledAmount) { dto.cancelledAmount = cancelledAmount; return this; }
        public Builder cancelledCount(long cancelledCount) { dto.cancelledCount = cancelledCount; return this; }

        public RevenueReportDTO build() { return dto; }
    }

    public double getTotalRevenue() { return totalRevenue; }
    public long getTotalBookings() { return totalBookings; }
    public double getAverageBookingAmount() { return averageBookingAmount; }
    public double getCancelledAmount() { return cancelledAmount; }
    public long getCancelledCount() { return cancelledCount; }
}
