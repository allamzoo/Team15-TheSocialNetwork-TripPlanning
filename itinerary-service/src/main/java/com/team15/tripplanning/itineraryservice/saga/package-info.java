/**
 * M3 — Slice S3 (itinerary-service): Saga orchestration state machine.
 *
 * <p><b>OWNED BY: S3</b> — pull request {@code feature/m3-s3-feign-amqp-itinerary}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code ItinerarySaga} — {@code @Service} that drives the completion saga:
 *       <pre>
 *       PLANNED ──complete()──► COMPLETING
 *           COMPLETING ──precheck pass──► PAYMENT_PENDING  (publishes ItineraryCompletedEvent)
 *           COMPLETING ──precheck fail──► PLANNED          (no event)
 *       PAYMENT_PENDING ──PaymentCompletedEvent──► PAID
 *       PAYMENT_PENDING ──PaymentFailedEvent────► PAYMENT_FAILED
 *       PAYMENT_FAILED  ──compensate()──────────► CANCELLED (publishes ItineraryCancelledEvent)
 *       PAID            ──PaymentRefundedEvent──► REFUNDED
 *       </pre>
 *   <li>{@code SagaPreCheck} — validates three invariants before saga publish:
 *       (1) user.status = ACTIVE via UserServiceClient,
 *       (2) destination.status = ACTIVE via DestinationServiceClient,
 *       (3) confirmedBookingCount ≥ 1 via BookingServiceClient.getConfirmedSummary.
 * </ul>
 *
 * <p>Status values {@code COMPLETING}, {@code PAYMENT_PENDING}, {@code PAID},
 * {@code PAYMENT_FAILED}, {@code REFUNDED} must be added to the {@code ItineraryStatus}
 * enum in the existing {@code model} package as part of this PR. The enum lives in
 * {@code model.Itinerary} — that is the only M1 file S3 is permitted to modify.
 */
package com.team15.tripplanning.itineraryservice.saga;
