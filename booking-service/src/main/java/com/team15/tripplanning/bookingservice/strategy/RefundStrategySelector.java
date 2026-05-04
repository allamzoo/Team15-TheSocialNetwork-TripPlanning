package com.team15.tripplanning.bookingservice.strategy;

import com.team15.tripplanning.bookingservice.dto.ItineraryRefundInfo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RefundStrategySelector {

    public RefundStrategy select(ItineraryRefundInfo itinerary) {
        if (itinerary == null || itinerary.getStartDate() == null) {
            return new NoRefundStrategy();
        }

        String status = itinerary.getStatus() != null
                ? itinerary.getStatus().trim().toUpperCase()
                : "";

        long daysBeforeDeparture = ChronoUnit.DAYS.between(
                LocalDate.now(),
                itinerary.getStartDate()
        );

        if (status.equals("IN_PROGRESS") || status.equals("COMPLETED") || daysBeforeDeparture <= 0) {
            return new NoRefundStrategy();
        }

        if (daysBeforeDeparture > 14) {
            return new EarlyCancellationRefundStrategy();
        }

        if (daysBeforeDeparture >= 7) {
            return new MidCancellationRefundStrategy();
        }

        return new LateCancellationRefundStrategy();
    }

    public long calculateDaysBeforeDeparture(ItineraryRefundInfo itinerary) {
        if (itinerary == null || itinerary.getStartDate() == null) {
            return 0;
        }

        return ChronoUnit.DAYS.between(LocalDate.now(), itinerary.getStartDate());
    }
}