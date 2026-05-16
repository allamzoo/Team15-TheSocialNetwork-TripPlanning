package com.team15.tripplanning.userservice.messaging.consumer;

import com.team15.tripplanning.userservice.config.UserEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Choreography-saga compensation handler (Section 8.1).
 *
 * When ItineraryEventConsumer fails to process a message (throws RuntimeException),
 * RabbitMQ dead-letters it to user.itinerary.saga-listener.dlq via the configured DLX.
 *
 * This listener picks up those dead-lettered messages, logs them for observability,
 * and applies the compensation step: the failed event is recorded so an operator
 * or automated process can retry or alert without losing the message.
 */
@Component
public class SagaDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaDlqConsumer.class);

    @RabbitListener(queues = UserEventConfig.SAGA_DLQ)
    public void onDeadLetter(Message message) {
        String routingKey  = message.getMessageProperties().getReceivedRoutingKey();
        String body        = new String(message.getBody(), StandardCharsets.UTF_8);
        String exchange    = message.getMessageProperties().getReceivedExchange();
        String originalKey = getHeader(message, "x-death[0].routing-keys");

        log.error("[SAGA-DLQ] Dead-lettered message received — compensation triggered. " +
                  "exchange={} routingKey={} originalRoutingKey={} body={}",
                  exchange, routingKey, originalKey, body);

        // Compensation: log the failure for operator visibility.
        // In a full saga implementation this could:
        //   - Emit an alert / PagerDuty notification
        //   - Write the failed event to a MongoDB dead-letter audit collection
        //   - Re-enqueue with a delay for automatic retry
        // For M3 S1-EVENTS scope: structured ERROR log is the required compensation step.
    }

    private String getHeader(Message message, String key) {
        Object val = message.getMessageProperties().getHeaders().get(key);
        return val != null ? val.toString() : "unknown";
    }
}
