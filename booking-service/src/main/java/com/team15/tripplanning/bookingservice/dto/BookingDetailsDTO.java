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

    private BookingDetailsDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BookingDetailsDTO dto = new BookingDetailsDTO();

        public Builder bookingId(Long bookingId) { dto.bookingId = bookingId; return this; }
        public Builder itineraryId(Long itineraryId) { dto.itineraryId = itineraryId; return this; }
        public Builder userId(Long userId) { dto.userId = userId; return this; }
        public Builder originalAmount(Double originalAmount) { dto.originalAmount = originalAmount; return this; }
        public Builder type(String type) { dto.type = type; return this; }
        public Builder status(String status) { dto.status = status; return this; }
        public Builder bookingDetails(Map<String, Object> bookingDetails) { dto.bookingDetails = bookingDetails; return this; }
        public Builder appliedCoupons(List<AppliedCouponDTO> appliedCoupons) { dto.appliedCoupons = appliedCoupons; return this; }
        public Builder totalDiscount(Double totalDiscount) { dto.totalDiscount = totalDiscount; return this; }
        public Builder finalAmount(Double finalAmount) { dto.finalAmount = finalAmount; return this; }

        public BookingDetailsDTO build() { return dto; }
    }

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
