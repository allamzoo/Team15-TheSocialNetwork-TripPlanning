/**
 * M3 — Slice S4 (activity-service): RabbitMQ event publishers.
 *
 * <p><b>OWNED BY: S4</b> — pull request {@code feature/m3-s4-feign-amqp-activity}.
 * Do not add files for any other slice in this package.
 *
 * <p>Classes to create in this package:
 * <ul>
 *   <li>{@code ActivityEventPublisher} — publishes to exchange {@code activity.events}:
 *       <ul>
 *         <li>{@code ActivityCreatedEvent} (routing key {@code activity.created})
 *             — fired on POST /api/activities after itinerary-existence check passes.
 *         <li>{@code ActivityLifecycleRecordedEvent} (routing key {@code activity.lifecycle-recorded})
 *             — fired each time a lifecycle entry is appended to Cassandra.
 *         <li>{@code ActivityCancelledEvent} (routing key {@code activity.cancelled})
 *             — fired on DELETE /api/activities/{id} or itinerary cancellation cascade.
 *       </ul>
 *   <li>{@code AmqpConfig} — declares {@code activity.events} TopicExchange bean.
 * </ul>
 */
package com.team15.tripplanning.activityservice.messaging.publisher;
