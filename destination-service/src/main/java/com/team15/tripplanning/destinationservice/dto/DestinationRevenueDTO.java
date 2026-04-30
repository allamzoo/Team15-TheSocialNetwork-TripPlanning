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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DestinationRevenueDTO dto = new DestinationRevenueDTO();

        public Builder destinationId(Long destinationId) {
            dto.destinationId = destinationId;
            return this;
        }

        public Builder name(String name) {
            dto.name = name;
            return this;
        }

        public Builder totalBookings(Long totalBookings) {
            dto.totalBookings = totalBookings;
            return this;
        }

        public Builder totalRevenue(Double totalRevenue) {
            dto.totalRevenue = totalRevenue;
            return this;
        }

        public Builder averageBookingAmount(Double averageBookingAmount) {
            dto.averageBookingAmount = averageBookingAmount;
            return this;
        }

        public DestinationRevenueDTO build() {
            return dto;
        }
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