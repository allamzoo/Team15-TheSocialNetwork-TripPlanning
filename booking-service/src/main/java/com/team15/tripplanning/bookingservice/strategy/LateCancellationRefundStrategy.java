package com.team15.tripplanning.bookingservice.strategy;

import com.team15.tripplanning.bookingservice.dto.ItineraryRefundInfo;
import com.team15.tripplanning.bookingservice.dto.RefundCancellationRequest;
import com.team15.tripplanning.bookingservice.model.Booking;

public class LateCancellationRefundStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(
            Booking booking,
            ItineraryRefundInfo itinerary,
            RefundCancellationRequest request
    ) {
        double amount = booking.getAmount() != null ? booking.getAmount() : 0.0;

        return new RefundResult(
                amount * 0.25,
                "LATE",
                "PARTIAL_REFUND",
                "LateCancellationRefundStrategy"
        );
    }
}