package com.team15.tripplanning.bookingservice.messaging.publisher;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S5-INFRA — RabbitMQ exchange, queue and binding declarations for booking-service.
 *
 * Topology owned by this service:
 *
 *   PUBLISHES TO  → payment.events (TopicExchange)
 *                    routing keys: payment.initiated | payment.completed |
 *                                  payment.failed   | payment.refunded
 *
 *   CONSUMES FROM → itinerary.events.booking-consumer  (Queue)
 *                    bound to: itinerary.events (TopicExchange, owned by itinerary-service)
 *                    routing key: itinerary.completed
 *
 * DLQ: itinerary.events.booking-consumer.dlq — receives messages that fail
 * all 3 retry attempts so they are not silently dropped.
 */
@Configuration
public class AmqpConfig {

    // ── Exchange names ─────────────────────────────────────────────────────────
    public static final String PAYMENT_EXCHANGE   = "payment.events";
    public static final String ITINERARY_EXCHANGE = "itinerary.events";

    // ── Queue names ────────────────────────────────────────────────────────────
    public static final String BOOKING_CONSUMER_QUEUE = "itinerary.events.booking-consumer";
    public static final String BOOKING_CONSUMER_DLQ   = "itinerary.events.booking-consumer.dlq";

    // ── Routing keys consumed ──────────────────────────────────────────────────
    public static final String ROUTING_ITINERARY_COMPLETED = "itinerary.completed";

    // ── Routing keys published ─────────────────────────────────────────────────
    public static final String ROUTING_PAYMENT_INITIATED  = "payment.initiated";
    public static final String ROUTING_PAYMENT_COMPLETED  = "payment.completed";
    public static final String ROUTING_PAYMENT_FAILED     = "payment.failed";
    public static final String ROUTING_PAYMENT_REFUNDED   = "payment.refunded";

    // ── Exchange beans ─────────────────────────────────────────────────────────

    /** TopicExchange this service publishes payment events to. */
    @Bean
    TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    /**
     * itinerary.events is declared by itinerary-service, but we redeclare it here
     * so the binding below can be created even when itinerary-service is not running.
     * RabbitMQ is idempotent about duplicate exchange declarations with identical arguments.
     */
    @Bean
    TopicExchange itineraryExchange() {
        return new TopicExchange(ITINERARY_EXCHANGE, true, false);
    }

    // ── Queue beans ────────────────────────────────────────────────────────────

    @Bean
    Queue bookingConsumerDlq() {
        return QueueBuilder.durable(BOOKING_CONSUMER_DLQ).build();
    }

    @Bean
    Queue bookingConsumerQueue() {
        return QueueBuilder.durable(BOOKING_CONSUMER_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", BOOKING_CONSUMER_DLQ)
                .build();
    }

    // ── Binding ────────────────────────────────────────────────────────────────

    @Bean
    Binding bookingConsumerBinding(Queue bookingConsumerQueue,
                                   TopicExchange itineraryExchange) {
        return BindingBuilder
                .bind(bookingConsumerQueue)
                .to(itineraryExchange)
                .with(ROUTING_ITINERARY_COMPLETED);
    }

    // ── Jackson message converter ──────────────────────────────────────────────

    @Bean
    Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
