/**
 * M3 — Slice S5 (booking-service): Feign client wiring.
 *
 * <p><b>OWNED BY: S5</b> — pull request {@code feature/m3-s5-feign-amqp-booking}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code FeignClientConfig} — {@code @Configuration} that forwards
 *       {@code X-User-Id} and {@code X-User-Role} on every outbound Feign call.
 * </ul>
 *
 * <p>The {@code @EnableFeignClients(basePackages = "com.team15.tripplanning.contracts.feign")}
 * annotation must be added to {@code BookingServiceApplication} in this PR.
 *
 * <p>Feign interfaces used by booking-service (defined in the {@code contracts} module):
 * <ul>
 *   <li>{@code UserServiceClient} — {@code getUser} (user-existence check on S5-F3)
 *   <li>{@code ItineraryServiceClient} — {@code getItinerary} (status + startDate for refund
 *       strategy and active-count for seasonal surcharge)
 *   <li>{@code DestinationServiceClient} — {@code batchGetDestinations} (name lookup for
 *       revenue grouping in S5-F10)
 * </ul>
 */
package com.team15.tripplanning.bookingservice.feign;
