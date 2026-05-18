package com.team15.tripplanning.activityservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ActivityEventConfig {

    // ── Exchange names ────────────────────────────────────────────────────────
    public static final String ACTIVITY_EXCHANGE    = "activity.events";
    public static final String ITINERARY_EXCHANGE   = "itinerary.events";

    // ── Dead-letter exchange ──────────────────────────────────────────────────
    public static final String ACTIVITY_DLX         = "activity.events.dlx";

    // ── Queue names ───────────────────────────────────────────────────────────
    public static final String ITINERARY_SAGA_QUEUE = "activity.itinerary.saga-listener";
    public static final String ITINERARY_SAGA_DLQ   = "activity.itinerary.saga-listener.dlq";

    // ── Routing keys S4 consumes ──────────────────────────────────────────────
    public static final String RK_ITINERARY_PLACED    = "itinerary.placed";
    public static final String RK_ITINERARY_COMPLETED = "itinerary.completed";
    public static final String RK_ITINERARY_CANCELLED = "itinerary.cancelled";

    // ── Routing keys S4 publishes ─────────────────────────────────────────────
    public static final String RK_ACTIVITY_CREATED             = "activity.created";
    public static final String RK_ACTIVITY_LIFECYCLE_RECORDED  = "activity.lifecycle-recorded";
    public static final String RK_ACTIVITY_CANCELLED           = "activity.cancelled";

    // ─────────────────────────────────────────────────────────────────────────
    // Exchanges
    // ─────────────────────────────────────────────────────────────────────────

    /** The exchange this service PUBLISHES to. */
    @Bean
    public TopicExchange activityEventsExchange() {
        return new TopicExchange(ACTIVITY_EXCHANGE, true, false);
    }

    /**
     * Reference to itinerary.events exchange — same name, Spring deduplicates.
     * Declared here so the consumer Binding can reference it.
     */
    @Bean
    public TopicExchange itineraryEventsExchangeRef() {
        return new TopicExchange(ITINERARY_EXCHANGE, true, false);
    }

    /** Dead-letter exchange for failed consumer messages. */
    @Bean
    public TopicExchange activityDlx() {
        return new TopicExchange(ACTIVITY_DLX, true, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consumer queue + DLQ
    // ─────────────────────────────────────────────────────────────────────────

    /** DLQ — receives messages after max-attempts: 3 are exhausted. */
    @Bean
    public Queue activityItinerarySagaDlq() {
        return QueueBuilder.durable(ITINERARY_SAGA_DLQ).build();
    }

    /**
     * Main consumer queue.
     * x-dead-letter-exchange + x-dead-letter-routing-key wire it to the DLQ
     * automatically when default-requeue-rejected: false + retry exhausted.
     */
    @Bean
    public Queue activityItinerarySagaQueue() {
        return QueueBuilder.durable(ITINERARY_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", ACTIVITY_DLX)
                .withArgument("x-dead-letter-routing-key", ITINERARY_SAGA_DLQ)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bindings — queue listens to all 3 itinerary routing keys
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public Binding bindItineraryPlaced() {
        return BindingBuilder
                .bind(activityItinerarySagaQueue())
                .to(itineraryEventsExchangeRef())
                .with(RK_ITINERARY_PLACED);
    }

    @Bean
    public Binding bindItineraryCompleted() {
        return BindingBuilder
                .bind(activityItinerarySagaQueue())
                .to(itineraryEventsExchangeRef())
                .with(RK_ITINERARY_COMPLETED);
    }

    @Bean
    public Binding bindItineraryCancelled() {
        return BindingBuilder
                .bind(activityItinerarySagaQueue())
                .to(itineraryEventsExchangeRef())
                .with(RK_ITINERARY_CANCELLED);
    }

    /** Routes dead-lettered messages from the DLX into the DLQ queue. */
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(activityItinerarySagaDlq())
                .to(activityDlx())
                .with(ITINERARY_SAGA_DLQ);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON message converter — all services use Jackson for AMQP payloads
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Primary ObjectMapper — guaranteed to be an autowire candidate even when
     * DataCassandraAutoConfiguration registers a non-candidate ObjectMapper internally.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        // Use Jackson2JsonMessageConverter (the non-deprecated form)
        return new Jackson2JsonMessageConverter();
    }
}