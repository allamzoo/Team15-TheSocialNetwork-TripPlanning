/**
 * M3 — Slice S5 (booking-service): RabbitMQ event publishers.
 *
 * <p><b>OWNED BY: S5</b> — pull request {@code feature/m3-s5-feign-amqp-booking}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code PaymentEventPublisher} — publishes to exchange {@code payment.events}:
 *       <ul>
 *         <li>{@code PaymentInitiatedEvent} (routing key {@code payment.initiated})
 *             — fired when a {@code Settlement} row transitions to {@code SETTLEMENT_PENDING}.
 *         <li>{@code PaymentCompletedEvent} (routing key {@code payment.completed})
 *             — fired when external payment gateway confirms success (simulated in M3).
 *         <li>{@code PaymentFailedEvent} (routing key {@code payment.failed})
 *             — fired when payment gateway reports failure.
 *         <li>{@code PaymentRefundedEvent} (routing key {@code payment.refunded})
 *             — fired when refund is processed via the refund-strategy tier.
 *       </ul>
 *   <li>{@code AmqpConfig} — declares {@code payment.events} TopicExchange bean.
 *       Also declares {@code itinerary.events.booking-consumer} queue bound to
 *       {@code itinerary.events} exchange (routing key {@code itinerary.completed})
 *       for the sibling consumer package.
 * </ul>
 */
package com.team15.tripplanning.bookingservice.messaging.publisher;
