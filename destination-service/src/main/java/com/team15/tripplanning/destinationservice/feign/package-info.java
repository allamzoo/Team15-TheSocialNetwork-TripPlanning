/**
 * M3 — Slice S2 (destination-service): Feign client wiring.
 *
 * <p><b>OWNED BY: S2</b> — pull request {@code feature/m3-s2-feign-amqp-destination}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code FeignClientConfig} — {@code @Configuration} supplying a
 *       {@code RequestInterceptor} that forwards {@code X-User-Id} and
 *       {@code X-User-Role} on every outbound Feign call.
 * </ul>
 *
 * <p>The {@code @EnableFeignClients(basePackages = "com.team15.tripplanning.contracts.feign")}
 * annotation must be added to {@code DestinationServiceApplication} in this PR.
 *
 * <p>Feign interfaces used by destination-service (defined in the {@code contracts} module):
 * <ul>
 *   <li>{@code ItineraryServiceClient} — {@code getDestinationBookingRevenue},
 *       {@code getDestinationActiveCount}, {@code getDestinationDashboardAggregate}
 *   <li>{@code UserServiceClient} — {@code getUser} (ADMIN role verification for
 *       destination status mutations)
 * </ul>
 */
package com.team15.tripplanning.destinationservice.feign;
