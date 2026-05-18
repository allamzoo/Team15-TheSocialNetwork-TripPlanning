/**
 * M3 — Slice S2 (destination-service): RabbitMQ event publishers.
 *
 * <p><b>OWNED BY: S2</b> — pull request {@code feature/m3-s2-feign-amqp-destination}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code DestinationEventPublisher} — publishes to exchange {@code destination.events}:
 *       <ul>
 *         <li>{@code DestinationStatusChangedEvent} (routing key {@code destination.status-changed})
 *             — fired whenever a destination transitions between ACTIVE / INACTIVE / SUSPENDED.
 *         <li>{@code DestinationRatedEvent} (routing key {@code destination.rated})
 *             — fired after a user rates a destination upon itinerary completion.
 *       </ul>
 *   <li>{@code AmqpConfig} — declares {@code destination.events} TopicExchange bean.
 *       Also declares {@code itinerary.events.destination-consumer} queue bound to
 *       {@code itinerary.events} exchange (routing key {@code itinerary.#}) for
 *       the sibling consumer package.
 * </ul>
 */
package com.team15.tripplanning.destinationservice.messaging.publisher;
