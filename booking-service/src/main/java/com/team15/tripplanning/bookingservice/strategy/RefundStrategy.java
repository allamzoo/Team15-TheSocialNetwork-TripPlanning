package com.team15.tripplanning.bookingservice.strategy;

import com.team15.tripplanning.bookingservice.dto.ItineraryRefundInfo;
import com.team15.tripplanning.bookingservice.dto.RefundCancellationRequest;
import com.team15.tripplanning.bookingservice.model.Booking;

public interface RefundStrategy {
    RefundResult calculateRefund(
            Booking booking,
            ItineraryRefundInfo itinerary,
            RefundCancellationRequest request
    );
}