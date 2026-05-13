/**
 * M3 — Slice S1 (user-service): RabbitMQ event publishers.
 *
 * <p><b>OWNED BY: S1</b> — pull request {@code feature/m3-s1-feign-amqp-user}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code UserEventPublisher} — publishes to exchange {@code user.events}:
 *       {@code UserRegisteredEvent} (routing key {@code user.registered}) and
 *       {@code UserDeactivatedEvent} (routing key {@code user.deactivated}).
 *   <li>{@code AmqpConfig} — declares {@code user.events} TopicExchange bean;
 *       no queues declared here (consumers declare their own queues).
 * </ul>
 *
 * <p>Downstream consumers: destination-service listens for {@code user.registered}
 * to warm Elasticsearch index; booking-service listens for {@code user.deactivated}
 * to cancel PENDING bookings.
 */
package com.team15.tripplanning.userservice.messaging.publisher;
