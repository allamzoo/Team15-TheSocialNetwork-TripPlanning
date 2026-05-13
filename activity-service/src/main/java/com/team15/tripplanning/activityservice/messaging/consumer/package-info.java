/**
 * M3 — Slice S4 (activity-service): RabbitMQ event consumers.
 *
 * <p><b>OWNED BY: S4</b> — pull request {@code feature/m3-s4-feign-amqp-activity}.
 * Do not add files for any other slice in this package.
 *
 * <p>Reserved for future cross-service event consumption. No consumers are required
 * in the S4 scope. This package exists to reserve the ownership boundary.
 *
 * <p>Potential future consumer: {@code ItineraryCancelledEvent} → cascade-cancel all
 * activities belonging to the cancelled itinerary. If added, declare queue binding
 * in the sibling {@code messaging.publisher.AmqpConfig}.
 */
package com.team15.tripplanning.activityservice.messaging.consumer;
