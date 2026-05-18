/**
 * M3 — Slice S3 (itinerary-service): RabbitMQ event publishers.
 *
 * <p><b>OWNED BY: S3</b> — pull request {@code feature/m3-s3-feign-amqp-itinerary}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code ItineraryEventPublisher} — publishes to exchange {@code itinerary.events}:
 *       <ul>
 *         <li>{@code ItineraryPlacedEvent} (routing key {@code itinerary.placed})
 *             — fired when status transitions to PLANNED (POST /api/itineraries).
 *         <li>{@code ItineraryCompletedEvent} (routing key {@code itinerary.completed})
 *             — fired when saga reaches PAID; carries {@code totalAmount}.
 *         <li>{@code ItineraryCancelledEvent} (routing key {@code itinerary.cancelled})
 *             — fired on explicit cancel or saga compensation.
 *       </ul>
 *   <li>{@code AmqpConfig} — declares {@code itinerary.events} TopicExchange bean.
 *       Also declares {@code payment.events.itinerary-consumer} queue bound to
 *       {@code payment.events} exchange (routing key {@code payment.#}) for the
 *       sibling consumer package.
 * </ul>
 *
 * <p>Publishing is transactional: {@code rabbitTemplate.convertAndSend()} is called
 * inside the same Spring transaction as the JPA status update; if the DB commit fails
 * the message is never sent (outbox pattern preferred for strict guarantees — deferred
 * to a hardening slice).
 */
package com.team15.tripplanning.itineraryservice.messaging.publisher;
