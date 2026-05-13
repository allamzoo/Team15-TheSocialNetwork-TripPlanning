/**
 * M3 — Slice S2 (destination-service): Active-itinerary-count feature.
 *
 * <p><b>OWNED BY: S2</b> — pull request {@code feature/m3-s2-feign-amqp-destination}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code ActiveCountController} — exposes:
 *       {@code GET /api/destinations/{id}/active-count} → {@code int}
 *   <li>{@code ActiveCountService} — reads the count from Redis (key
 *       {@code dest:active:{id}}) maintained by the AMQP consumer in
 *       {@code messaging.consumer.ItineraryEventConsumer}. Falls back to
 *       {@code ItineraryServiceClient.getDestinationActiveCount} on cache miss,
 *       then repopulates Redis.
 * </ul>
 */
package com.team15.tripplanning.destinationservice.feature.activecount;
