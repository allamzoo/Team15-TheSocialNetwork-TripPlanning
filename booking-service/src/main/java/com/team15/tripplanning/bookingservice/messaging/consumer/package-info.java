/**
 * M3 — Slice S5 (booking-service): RabbitMQ event consumers (saga trigger).
 *
 * <p><b>OWNED BY: S5</b> — pull request {@code feature/m3-s5-feign-amqp-booking}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code ItineraryEventConsumer} — {@code @RabbitListener} on queue
 *       {@code itinerary.events.booking-consumer}; handles:
 *       <ul>
 *         <li>{@code ItineraryCompletedEvent} → creates a {@code Settlement} row
 *             (status {@code SETTLEMENT_PENDING}), then publishes
 *             {@code PaymentInitiatedEvent} to kick off the payment sub-saga.
 *       </ul>
 * </ul>
 *
 * <p>Idempotency: unique index on {@code settlements.itinerary_id} prevents duplicate
 * settlement rows if the message is delivered more than once.
 * Queue binding is declared in the sibling {@code messaging.publisher.AmqpConfig}.
 */
package com.team15.tripplanning.bookingservice.messaging.consumer;
