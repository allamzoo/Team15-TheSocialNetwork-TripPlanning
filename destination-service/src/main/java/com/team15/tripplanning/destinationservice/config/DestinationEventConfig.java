package com.team15.tripplanning.destinationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DestinationEventConfig {

    // ===== Exchange this service publishes to =====
    @Bean
    public TopicExchange destinationExchange() {
        return new TopicExchange("destination.events");
    }

    // ===== Consumer queue with dead‑letter configuration =====
    @Bean
    public Queue destinationSagaQueue() {
        return QueueBuilder.durable("destination.itinerary.saga-listener")
                .withArgument("x-dead-letter-exchange", "destination.itinerary.saga-dlx")
                .withArgument("x-dead-letter-routing-key", "destination.itinerary.saga-dlq")
                .build();
    }

    // ===== Dead‑letter exchange =====
    @Bean
    public DirectExchange sagaDlx() {
        return new DirectExchange("destination.itinerary.saga-dlx");
    }

    // ===== Dead‑letter queue =====
    @Bean
    public Queue sagaDlq() {
        return new Queue("destination.itinerary.saga-listener.dlq");
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(sagaDlq())
                .to(sagaDlx())
                .with("destination.itinerary.saga-dlq");
    }

    // ===== Bindings from itinerary.events to your consumer queue =====
    @Bean
    public Binding placedBinding() {
        return BindingBuilder.bind(destinationSagaQueue())
                .to(new TopicExchange("itinerary.events"))
                .with("itinerary.placed");
    }

    @Bean
    public Binding completedBinding() {
        return BindingBuilder.bind(destinationSagaQueue())
                .to(new TopicExchange("itinerary.events"))
                .with("itinerary.completed");
    }

    @Bean
    public Binding cancelledBinding() {
        return BindingBuilder.bind(destinationSagaQueue())
                .to(new TopicExchange("itinerary.events"))
                .with("itinerary.cancelled");
    }
}