/**
 * M3 — Slice S1 (user-service): Trip-summary and booking-summary aggregate endpoints.
 *
 * <p><b>OWNED BY: S1</b> — pull request {@code feature/m3-s1-feign-amqp-user}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code TripSummaryController} — exposes:
 *       <ul>
 *         <li>{@code GET /api/users/{userId}/trip-summary} → {@code UserTripSummaryAggregateDTO}
 *             (delegates to {@code ItineraryServiceClient.getUserItinerarySummary})
 *         <li>{@code GET /api/users/{userId}/booking-summary} → {@code UserBookingTotalDTO}
 *             (delegates to {@code BookingServiceClient.getUserBookingTotal})
 *       </ul>
 *   <li>{@code TripSummaryService} — orchestrates the two Feign calls, falls back to
 *       {@code UserTripSummaryAggregateDTO.empty()} / {@code UserBookingTotalDTO.empty()}
 *       when downstream returns 404 (user has no trips yet).
 * </ul>
 *
 * <p>These endpoints are the primary M3 consumer-facing additions for S1.
 * All other user-service endpoints (CRUD, auth, saved-destinations) remain in
 * the existing {@code controller} / {@code service} packages and are NOT touched by S1.
 */
package com.team15.tripplanning.userservice.feature.tripsummary;
