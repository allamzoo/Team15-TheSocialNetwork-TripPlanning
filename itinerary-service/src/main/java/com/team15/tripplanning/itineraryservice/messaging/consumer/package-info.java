/**
 * M3 — Slice S3 (itinerary-service): RabbitMQ event consumers (saga callbacks).
 *
 * <p><b>OWNED BY: S3</b> — pull request {@code feature/m3-s3-feign-amqp-itinerary}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code PaymentEventConsumer} — {@code @RabbitListener} on queue
 *       {@code payment.events.itinerary-consumer}; handles saga callback messages:
 *       <ul>
 *         <li>{@code PaymentCompletedEvent} → transitions itinerary from
 *             {@code PAYMENT_PENDING} to {@code PAID} and publishes
 *             {@code ItineraryCompletedEvent}.
 *         <li>{@code PaymentFailedEvent} → transitions to {@code PAYMENT_FAILED};
 *             triggers compensation (publishes {@code ItineraryCancelledEvent}).
 *         <li>{@code PaymentRefundedEvent} → transitions to {@code REFUNDED}.
 *       </ul>
 * </ul>
 *
 * <p>Idempotency: each handler checks the current status before mutating.
 * If already in the target terminal state the message is ACK-ed and dropped.
 * Queue binding is declared in the sibling {@code messaging.publisher.AmqpConfig}.
 */
package com.team15.tripplanning.itineraryservice.messaging.consumer;
