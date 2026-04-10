package com.team15.tripplanning.destinationservice.dto;

public class DestinationRevenueDTO {
    private Long destinationId;
    private String name;
    private Long totalBookings;
    private Double totalRevenue;
    private Double averageBookingAmount;

    public DestinationRevenueDTO() {
    }

    public DestinationRevenueDTO(Long destinationId, String name, Long totalBookings, Double totalRevenue, Double averageBookingAmount) {
        this.destinationId = destinationId;
        this.name = name;
        this.totalBookings = totalBookings;
        this.totalRevenue = totalRevenue;
        this.averageBookingAmount = averageBookingAmount;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getAverageBookingAmount() {
        return averageBookingAmount;
    }

    public void setAverageBookingAmount(Double averageBookingAmount) {
        this.averageBookingAmount = averageBookingAmount;
    }
}