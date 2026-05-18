/**
 * M3 — Slice S2 (destination-service): Booking-revenue chain feature.
 *
 * <p><b>OWNED BY: S2</b> — pull request {@code feature/m3-s2-feign-amqp-destination}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code DestinationRevenueController} — exposes:
 *       {@code GET /api/destinations/{id}/booking-revenue?startDate=&endDate=}
 *       → {@code DestinationBookingRevenueAggregateDTO}
 *   <li>{@code DestinationRevenueService} — calls
 *       {@code BookingServiceClient.aggregateByItineraries} with the destination's
 *       itinerary IDs to compute confirmed revenue in the requested date window.
 *       Result is cached in Redis with key {@code dest:revenue:{id}:{start}:{end}}.
 * </ul>
 *
 * <p>Do NOT add dashboard or active-count logic here; those live in their own sibling packages.
 */
package com.team15.tripplanning.destinationservice.feature.revenue;
