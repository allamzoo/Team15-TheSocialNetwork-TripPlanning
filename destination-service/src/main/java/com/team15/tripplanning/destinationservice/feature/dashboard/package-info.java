/**
 * M3 — Slice S2 (destination-service): Dashboard-aggregate feature.
 *
 * <p><b>OWNED BY: S2</b> — pull request {@code feature/m3-s2-feign-amqp-destination}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code DashboardController} — exposes:
 *       {@code GET /api/destinations/{id}/dashboard-aggregate}
 *       → {@code DestinationDashboardAggregateDTO}
 *   <li>{@code DashboardService} — fan-out Feign call to
 *       {@code ItineraryServiceClient.getDestinationDashboardAggregate}; assembles
 *       the composite DTO ({@code activeCount}, {@code completedCount}, {@code cancelledCount}).
 *       Cached in Redis with key {@code dest:dashboard:{id}}, TTL 5 minutes.
 * </ul>
 */
package com.team15.tripplanning.destinationservice.feature.dashboard;
