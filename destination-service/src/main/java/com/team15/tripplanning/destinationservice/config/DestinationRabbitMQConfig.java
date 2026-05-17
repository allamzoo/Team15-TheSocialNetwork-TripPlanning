package com.team15.tripplanning.destinationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DestinationRabbitMQConfig {

    // ── Exchange names ────────────────────────────────────────────────────────
    public static final String DESTINATION_EVENTS_EXCHANGE = "destination.events";
    public static final String ITINERARY_EVENTS_EXCHANGE   = "itinerary.events";

    // ── Routing keys published by destination-service ─────────────────────────
    public static final String ROUTING_KEY_STATUS_CHANGED = "destination.status-changed";
    public static final String ROUTING_KEY_RATED          = "destination.rated";

    // ── Consumer queue ────────────────────────────────────────────────────────
    public static final String ITINERARY_SAGA_QUEUE = "destination.itinerary.saga-listener";
    public static final String ITINERARY_SAGA_DLQ   = "destination.itinerary.saga-listener.dlq";

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange destinationEventsExchange() {
        return new TopicExchange(DESTINATION_EVENTS_EXCHANGE, true, false);
    }

    /** Declared here so destination-service can bind its consumer queue to it. */
    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange(ITINERARY_EVENTS_EXCHANGE, true, false);
    }

    // ── Queues ────────────────────────────────────────────────────────────────

    @Bean
    public Queue itineraryDlq() {
        return QueueBuilder.durable(ITINERARY_SAGA_DLQ).build();
    }

    @Bean
    public Queue itinerarySagaQueue() {
        return QueueBuilder.durable(ITINERARY_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", ITINERARY_SAGA_DLQ)
                .build();
    }

    // ── Bindings — destination-service listens to itinerary lifecycle events ──

    @Bean
    public Binding bindingItineraryPlaced(Queue itinerarySagaQueue,
                                          TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(itinerarySagaQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.placed");
    }

    @Bean
    public Binding bindingItineraryCompleted(Queue itinerarySagaQueue,
                                             TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(itinerarySagaQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.completed");
    }

    @Bean
    public Binding bindingItineraryCancelled(Queue itinerarySagaQueue,
                                             TopicExchange itineraryEventsExchange) {
        return BindingBuilder.bind(itinerarySagaQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.cancelled");
    }

    // ── Jackson JSON message converter ────────────────────────────────────────

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }

    /** Explicit factory so @RabbitListener container uses the same Jackson converter. */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        return factory;
    }
}
