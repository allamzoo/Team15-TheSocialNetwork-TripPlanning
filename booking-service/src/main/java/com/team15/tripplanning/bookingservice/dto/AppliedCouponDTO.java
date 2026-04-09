package com.team15.tripplanning.bookingservice.dto;

import java.time.LocalDateTime;

public class AppliedCouponDTO {
    private String couponCode;
    private String discountType;
    private Double discountApplied;
    private LocalDateTime appliedAt;

    // getters & setters
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public Double getDiscountApplied() { return discountApplied; }
    public void setDiscountApplied(Double discountApplied) { this.discountApplied = discountApplied; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}