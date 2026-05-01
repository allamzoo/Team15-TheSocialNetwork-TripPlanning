package com.team15.tripplanning.bookingservice.strategy;

public class RefundResult {
    private final double refundAmount;
    private final String tier;
    private final String reasonCode;
    private final String strategyName;

    public RefundResult(double refundAmount, String tier, String reasonCode, String strategyName) {
        this.refundAmount = refundAmount;
        this.tier = tier;
        this.reasonCode = reasonCode;
        this.strategyName = strategyName;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public String getTier() {
        return tier;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getStrategyName() {
        return strategyName;
    }
}