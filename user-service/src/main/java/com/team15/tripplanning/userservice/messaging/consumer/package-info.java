/**
 * M3 — Slice S1 (user-service): RabbitMQ event consumers.
 *
 * <p><b>OWNED BY: S1</b> — pull request {@code feature/m3-s1-feign-amqp-user}.
 * Do not add files for any other slice in this package.
 *
 * <p>Reserved for future cross-service event consumption. No consumers are required
 * in the S1 scope; this package exists to reserve the ownership boundary so that
 * any inbound AMQP listener added later lives here and not in another package.
 *
 * <p>If a consumer is added, declare its queue binding in {@code AmqpConfig} inside
 * the {@code messaging.publisher} sibling package and annotate the listener method
 * with {@code @RabbitListener(queues = "...")}.
 */
package com.team15.tripplanning.userservice.messaging.consumer;
