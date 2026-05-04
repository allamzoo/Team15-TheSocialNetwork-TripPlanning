package com.team15.tripplanning.bookingservice.strategy;

import com.team15.tripplanning.bookingservice.dto.ItineraryRefundInfo;
import com.team15.tripplanning.bookingservice.dto.RefundCancellationRequest;
import com.team15.tripplanning.bookingservice.model.Booking;

public class NoRefundStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(
            Booking booking,
            ItineraryRefundInfo itinerary,
            RefundCancellationRequest request
    ) {
        return new RefundResult(
                0.0,
                "NONE",
                "TRIP_ALREADY_STARTED_OR_COMPLETED",
                "NoRefundStrategy"
        );
    }
}