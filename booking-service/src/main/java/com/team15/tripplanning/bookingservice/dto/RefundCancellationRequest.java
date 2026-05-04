package com.team15.tripplanning.bookingservice.dto;

public class RefundCancellationRequest {
    private String reason;

    public RefundCancellationRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}