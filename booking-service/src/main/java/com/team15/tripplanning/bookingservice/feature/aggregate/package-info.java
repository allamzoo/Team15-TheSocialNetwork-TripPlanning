/**
 * M3 — Slice S5 (booking-service): Booking aggregate endpoints.
 *
 * <p><b>OWNED BY: S5</b> — pull request {@code feature/m3-s5-feign-amqp-booking}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code BookingAggregateController} — exposes:
 *       <ul>
 *         <li>{@code GET /api/bookings/user/{userId}/total?startDate=&endDate=}
 *             → {@code UserBookingTotalDTO} (called by user-service S1 via Feign)
 *         <li>{@code GET /api/bookings/itinerary/{itineraryId}/confirmed-summary}
 *             → {@code ConfirmedSummaryDTO} (called by itinerary-service S3 saga pre-check)
 *         <li>{@code POST /api/bookings/aggregate-by-itineraries}
 *             body: {@code BookingAggregateRequest} → {@code ItineraryAggregateDTO}
 *             (called by itinerary-service S3 for destination revenue chain)
 *       </ul>
 *   <li>{@code BookingAggregateService} — JPA aggregate queries against the
 *       existing {@code bookings} table. Results for {@code /user/{id}/total}
 *       are cached in Redis (key {@code booking:total:{userId}:{start}:{end}}, TTL 2 min).
 * </ul>
 *
 * <p>These endpoints are the primary Feign-callable surface of booking-service.
 * They must exist before S1 and S3 can be verified end-to-end against a live booking-service.
 */
package com.team15.tripplanning.bookingservice.feature.aggregate;
