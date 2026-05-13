/**
 * M3 — Slice S2 (destination-service): RabbitMQ event consumers.
 *
 * <p><b>OWNED BY: S2</b> — pull request {@code feature/m3-s2-feign-amqp-destination}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code ItineraryEventConsumer} — {@code @RabbitListener} on queue
 *       {@code itinerary.events.destination-consumer}; handles:
 *       <ul>
 *         <li>{@code ItineraryPlacedEvent} → increments destination's
 *             {@code activeItineraryCount} cached in Redis.
 *         <li>{@code ItineraryCompletedEvent} → decrements active count,
 *             triggers {@code DestinationRatedEvent} publication.
 *         <li>{@code ItineraryCancelledEvent} → decrements active count,
 *             evicts destination cache entry.
 *       </ul>
 * </ul>
 *
 * <p>Queue binding is declared in the sibling {@code messaging.publisher.AmqpConfig}.
 * Idempotency: check {@code destinationId + itineraryId} in Redis before mutating.
 */
package com.team15.tripplanning.destinationservice.messaging.consumer;
