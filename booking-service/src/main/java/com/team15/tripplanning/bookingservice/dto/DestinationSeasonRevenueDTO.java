package com.team15.tripplanning.bookingservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DestinationSeasonRevenueDTO {
    private Long destinationId;
    private String destinationName;
    private Double totalRevenue;
    private Double baseRevenue;
    private Double surchargeRevenue;
    private Long peakBookingCount;
    private Long offPeakBookingCount;
}