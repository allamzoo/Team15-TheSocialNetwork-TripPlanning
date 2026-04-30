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

    private CouponUsageDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final CouponUsageDTO dto = new CouponUsageDTO();

        public Builder couponId(Long couponId) { dto.couponId = couponId; return this; }
        public Builder code(String code) { dto.code = code; return this; }
        public Builder discountType(String discountType) { dto.discountType = discountType; return this; }
        public Builder discountValue(Double discountValue) { dto.discountValue = discountValue; return this; }
        public Builder timesUsed(Integer timesUsed) { dto.timesUsed = timesUsed; return this; }
        public Builder totalDiscountGiven(Double totalDiscountGiven) { dto.totalDiscountGiven = totalDiscountGiven; return this; }
        public Builder active(Boolean active) { dto.active = active; return this; }
        public Builder expired(Boolean expired) { dto.expired = expired; return this; }

        public CouponUsageDTO build() { return dto; }
    }

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
