package com.team15.tripplanning.bookingservice.dto;

public class CouponUsageDTO {
    private Long couponId;
    private String code;
    private String discountType;
    private Double discountValue;
    private Integer timesUsed;
    private Double totalDiscountGiven;
    private Boolean active;
    private Boolean expired;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long couponId;
        private String code;
        private String discountType;
        private Double discountValue;
        private Integer timesUsed;
        private Double totalDiscountGiven;
        private Boolean active;
        private Boolean expired;

        public Builder couponId(Long couponId) { this.couponId = couponId; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder discountType(String discountType) { this.discountType = discountType; return this; }
        public Builder discountValue(Double discountValue) { this.discountValue = discountValue; return this; }
        public Builder timesUsed(Integer timesUsed) { this.timesUsed = timesUsed; return this; }
        public Builder totalDiscountGiven(Double totalDiscountGiven) { this.totalDiscountGiven = totalDiscountGiven; return this; }
        public Builder active(Boolean active) { this.active = active; return this; }
        public Builder expired(Boolean expired) { this.expired = expired; return this; }

        public CouponUsageDTO build() {
            CouponUsageDTO dto = new CouponUsageDTO();
            dto.setCouponId(couponId);
            dto.setCode(code);
            dto.setDiscountType(discountType);
            dto.setDiscountValue(discountValue);
            dto.setTimesUsed(timesUsed);
            dto.setTotalDiscountGiven(totalDiscountGiven);
            dto.setActive(active);
            dto.setExpired(expired);
            return dto;
        }
    }

    // getters & setters
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }

    public Integer getTimesUsed() { return timesUsed; }
    public void setTimesUsed(Integer timesUsed) { this.timesUsed = timesUsed; }

    public Double getTotalDiscountGiven() { return totalDiscountGiven; }
    public void setTotalDiscountGiven(Double totalDiscountGiven) { this.totalDiscountGiven = totalDiscountGiven; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getExpired() { return expired; }
    public void setExpired(Boolean expired) { this.expired = expired; }
}