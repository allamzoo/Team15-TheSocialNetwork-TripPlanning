package com.team15.tripplanning.itineraryservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItineraryRabbitMQConfig {

    // ── Queue / exchange name constants ──────────────────────────────────────
    public static final String ITINERARY_EXCHANGE       = "itinerary.events";

    public static final String USER_EVENTS_QUEUE        = "itinerary.user-events-listener";
    public static final String USER_EVENTS_DLQ          = "itinerary.user-events-listener.dlq";
    private static final String USER_EVENTS_DLX         = "itinerary.user-events.dlx";

    public static final String SAGA_FEEDBACK_QUEUE      = "itinerary.saga-feedback";
    public static final String SAGA_FEEDBACK_DLQ        = "itinerary.saga-feedback.dlq";
    private static final String SAGA_FEEDBACK_DLX       = "itinerary.saga.dlx";

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // ── Producer: itinerary.events TopicExchange ──────────────────────────────
    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange(ITINERARY_EXCHANGE, true, false);
    }

    // ── Consumer queue 1: user / destination / activity events ───────────────
    @Bean
    public TopicExchange userEventsDlx() {
        return new TopicExchange(USER_EVENTS_DLX, true, false);
    }

    @Bean
    public Queue userEventsDlq() {
        return QueueBuilder.durable(USER_EVENTS_DLQ).build();
    }

    @Bean
    public Binding userEventsDlqBinding() {
        return BindingBuilder.bind(userEventsDlq()).to(userEventsDlx()).with("#");
    }

    @Bean
    public Queue userEventsQueue() {
        return QueueBuilder.durable(USER_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .build();
    }

    // Source exchanges — Spring deduplicates if other services declare the same name
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange("user.events", true, false);
    }

    @Bean
    public TopicExchange destinationEventsExchange() {
        return new TopicExchange("destination.events", true, false);
    }

    @Bean
    public TopicExchange activityEventsExchange() {
        return new TopicExchange("activity.events", true, false);
    }

    @Bean
    public Binding bindUserEventsToListener() {
        return BindingBuilder.bind(userEventsQueue()).to(userEventsExchange()).with("user.#");
    }

    @Bean
    public Binding bindDestinationEventsToListener() {
        return BindingBuilder.bind(userEventsQueue()).to(destinationEventsExchange()).with("destination.#");
    }

    @Bean
    public Binding bindActivityEventsToListener() {
        return BindingBuilder.bind(userEventsQueue()).to(activityEventsExchange()).with("activity.#");
    }

    // ── Consumer queue 2: payment events (saga feedback) ─────────────────────
    @Bean
    public TopicExchange sagaFeedbackDlx() {
        return new TopicExchange(SAGA_FEEDBACK_DLX, true, false);
    }

    @Bean
    public Queue sagaFeedbackDlq() {
        return QueueBuilder.durable(SAGA_FEEDBACK_DLQ).build();
    }

    @Bean
    public Binding sagaFeedbackDlqBinding() {
        return BindingBuilder.bind(sagaFeedbackDlq()).to(sagaFeedbackDlx()).with("#");
    }

    @Bean
    public Queue sagaFeedbackQueue() {
        return QueueBuilder.durable(SAGA_FEEDBACK_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_FEEDBACK_DLX)
                .build();
    }

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange("payment.events", true, false);
    }

    @Bean
    public Binding bindPaymentEventsToSaga() {
        return BindingBuilder.bind(sagaFeedbackQueue()).to(paymentEventsExchange()).with("payment.#");
    }
}
