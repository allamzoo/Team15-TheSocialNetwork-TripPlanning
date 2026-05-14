package com.team15.tripplanning.bookingservice.model;

/**
 * Lifecycle states of a settlement row (§1.5).
 *
 * PENDING      — row created by itinerary.completed consumer; awaiting customer action
 * PROCESSING   — SELECT FOR UPDATE lock claimed; concurrent retry sees this → 409
 * COMPLETED    — settlement succeeded; payment.completed published
 * FAILED       — settlement rejected; payment.failed published
 * REFUNDED     — itinerary.cancelled consumer refunded the trip; payment.refunded published
 */
public enum SettlementStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}
