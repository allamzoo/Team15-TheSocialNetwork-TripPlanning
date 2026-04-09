package com.team15.tripplanning.bookingservice.dto;

import java.util.List;
import java.util.Map;

public class BookingDetailsDTO {
    private Long bookingId;
    private Long itineraryId;
    private Long userId;
    private Double originalAmount;
    private String type;
    private String status;
    private Map<String, Object> bookingDetails;

    private List<AppliedCouponDTO> appliedCoupons;

    private Double totalDiscount;
    private Double finalAmount;

    // getters & setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getItineraryId() { return itineraryId; }
    public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Double originalAmount) { this.originalAmount = originalAmount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getBookingDetails() { return bookingDetails; }
    public void setBookingDetails(Map<String, Object> bookingDetails) { this.bookingDetails = bookingDetails; }

    public List<AppliedCouponDTO> getAppliedCoupons() { return appliedCoupons; }
    public void setAppliedCoupons(List<AppliedCouponDTO> appliedCoupons) { this.appliedCoupons = appliedCoupons; }

    public Double getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(Double totalDiscount) { this.totalDiscount = totalDiscount; }

    public Double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(Double finalAmount) { this.finalAmount = finalAmount; }
}