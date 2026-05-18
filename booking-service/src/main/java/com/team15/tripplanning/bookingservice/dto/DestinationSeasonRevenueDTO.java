package com.team15.tripplanning.bookingservice.dto;

public class DestinationSeasonRevenueDTO {
    private Long destinationId;
    private String destinationName;
    private Double totalRevenue;
    private Double baseRevenue;
    private Double surchargeRevenue;
    private Long peakBookingCount;
    private Long offPeakBookingCount;

    private DestinationSeasonRevenueDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DestinationSeasonRevenueDTO obj = new DestinationSeasonRevenueDTO();

        public Builder destinationId(Long v)          { obj.destinationId = v; return this; }
        public Builder destinationName(String v)      { obj.destinationName = v; return this; }
        public Builder totalRevenue(Double v)         { obj.totalRevenue = v; return this; }
        public Builder baseRevenue(Double v)          { obj.baseRevenue = v; return this; }
        public Builder surchargeRevenue(Double v)     { obj.surchargeRevenue = v; return this; }
        public Builder peakBookingCount(Long v)       { obj.peakBookingCount = v; return this; }
        public Builder offPeakBookingCount(Long v)    { obj.offPeakBookingCount = v; return this; }

        public DestinationSeasonRevenueDTO build()    { return obj; }
    }

    public Long getDestinationId()          { return destinationId; }
    public String getDestinationName()      { return destinationName; }
    public Double getTotalRevenue()         { return totalRevenue; }
    public Double getBaseRevenue()          { return baseRevenue; }
    public Double getSurchargeRevenue()     { return surchargeRevenue; }
    public Long getPeakBookingCount()       { return peakBookingCount; }
    public Long getOffPeakBookingCount()    { return offPeakBookingCount; }

    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    public void setBaseRevenue(Double baseRevenue) { this.baseRevenue = baseRevenue; }
    public void setSurchargeRevenue(Double surchargeRevenue) { this.surchargeRevenue = surchargeRevenue; }
    public void setPeakBookingCount(Long peakBookingCount) { this.peakBookingCount = peakBookingCount; }
    public void setOffPeakBookingCount(Long offPeakBookingCount) { this.offPeakBookingCount = offPeakBookingCount; }
}
