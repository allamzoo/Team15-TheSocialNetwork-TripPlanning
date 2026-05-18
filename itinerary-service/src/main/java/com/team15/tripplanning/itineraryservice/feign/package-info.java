/**
 * M3 — Slice S3 (itinerary-service): Feign client wiring.
 *
 * <p><b>OWNED BY: S3</b> — pull request {@code feature/m3-s3-feign-amqp-itinerary}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code FeignClientConfig} — {@code @Configuration} that forwards
 *       {@code X-User-Id} and {@code X-User-Role} on every outbound Feign call.
 * </ul>
 *
 * <p>The {@code @EnableFeignClients(basePackages = "com.team15.tripplanning.contracts.feign")}
 * annotation must be added to {@code ItineraryServiceApplication} in this PR.
 *
 * <p>Feign interfaces used by itinerary-service (defined in the {@code contracts} module):
 * <ul>
 *   <li>{@code UserServiceClient} — {@code getUser} (pre-check: status = ACTIVE)
 *   <li>{@code DestinationServiceClient} — {@code getDestination} (pre-check: status = ACTIVE),
 *       {@code batchGetDestinations} (recommendation enrichment)
 *   <li>{@code BookingServiceClient} — {@code getConfirmedSummary} (saga trigger: count ≥ 1),
 *       {@code aggregateByItineraries} (revenue chain for S2 delegates)
 * </ul>
 */
package com.team15.tripplanning.itineraryservice.feign;
