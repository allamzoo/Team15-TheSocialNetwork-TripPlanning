/**
 * M3 — Slice S1 (user-service): Feign client wiring.
 *
 * <p><b>OWNED BY: S1</b> — pull request {@code feature/m3-s1-feign-amqp-user}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code FeignClientConfig} — {@code @Configuration} that supplies a custom
 *       {@code RequestInterceptor} forwarding {@code X-User-Id} and {@code X-User-Role}
 *       headers from the incoming request to every outbound Feign call.
 * </ul>
 *
 * <p>The {@code @EnableFeignClients(basePackages = "com.team15.tripplanning.contracts.feign")}
 * annotation must be added to {@code UserServiceApplication} in this PR.
 *
 * <p>Feign interfaces used by user-service (defined in the {@code contracts} module):
 * <ul>
 *   <li>{@code ItineraryServiceClient} — {@code getUserItinerarySummary}, {@code getActiveItineraryCount},
 *       {@code getCompletedItineraryCount}
 *   <li>{@code BookingServiceClient} — {@code getUserBookingTotal}
 * </ul>
 */
package com.team15.tripplanning.userservice.feign;
