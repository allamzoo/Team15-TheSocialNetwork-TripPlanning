package com.team15.tripplanning.bookingservice.dto;

public class RevenueReportDTO {

    private double totalRevenue;
    private long totalBookings;
    private double averageBookingAmount;
    private double cancelledAmount;
    private long cancelledCount;

    public RevenueReportDTO(double totalRevenue, long totalBookings,
                            double averageBookingAmount,
                            double cancelledAmount, long cancelledCount) {
        this.totalRevenue = totalRevenue;
        this.totalBookings = totalBookings;
        this.averageBookingAmount = averageBookingAmount;
        this.cancelledAmount = cancelledAmount;
        this.cancelledCount = cancelledCount;
    }

    public double getTotalRevenue() { return totalRevenue; }
    public long getTotalBookings() { return totalBookings; }
    public double getAverageBookingAmount() { return averageBookingAmount; }
    public double getCancelledAmount() { return cancelledAmount; }
    public long getCancelledCount() { return cancelledCount; }
}