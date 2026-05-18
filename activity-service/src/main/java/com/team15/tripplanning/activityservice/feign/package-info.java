/**
 * M3 — Slice S4 (activity-service): Feign client wiring.
 *
 * <p><b>OWNED BY: S4</b> — pull request {@code feature/m3-s4-feign-amqp-activity}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code FeignClientConfig} — {@code @Configuration} that forwards
 *       {@code X-User-Id} and {@code X-User-Role} on every outbound Feign call.
 * </ul>
 *
 * <p>The {@code @EnableFeignClients(basePackages = "com.team15.tripplanning.contracts.feign")}
 * annotation must be added to {@code ActivityServiceApplication} in this PR.
 *
 * <p>Feign interfaces used by activity-service (defined in the {@code contracts} module):
 * <ul>
 *   <li>{@code ItineraryServiceClient} — {@code getItinerary} (itinerary-existence and
 *       status check before creating an Activity or recording a lifecycle event).
 * </ul>
 */
package com.team15.tripplanning.activityservice.feign;
