package com.team15.tripplanning.itineraryservice.model;

public enum ItineraryStatus {
    DRAFT,
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    // M3 saga states
    COMPLETING,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    REFUNDED
}

