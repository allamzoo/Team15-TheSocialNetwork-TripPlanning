package com.team15.tripplanning.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class UserEventConfig {

    public static final String USER_EVENTS_EXCHANGE      = "user.events";
    public static final String ITINERARY_EVENTS_EXCHANGE = "itinerary.events";
    public static final String SAGA_QUEUE                = "user.itinerary.saga-listener";
    public static final String SAGA_DLQ                  = "user.itinerary.saga-listener.dlq";
    private static final String SAGA_DLX                 = "user.itinerary.saga-listener.dlx";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange(ITINERARY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange sagaDlx() {
        return new DirectExchange(SAGA_DLX, true, false);
    }

    @Bean
    public Queue sagaDlq() {
        return QueueBuilder.durable(SAGA_DLQ).build();
    }

    @Bean
    public Binding sagaDlqBinding() {
        return BindingBuilder.bind(sagaDlq()).to(sagaDlx()).with(SAGA_DLQ);
    }

    @Bean
    public Queue sagaQueue() {
        return QueueBuilder.durable(SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_DLX)
                .withArgument("x-dead-letter-routing-key", SAGA_DLQ)
                .build();
    }

    @Bean
    public Binding bindCompleted() {
        return BindingBuilder.bind(sagaQueue())
                .to(itineraryEventsExchange())
                .with("itinerary.completed");
    }

    @Bean
    public Binding bindCancelled() {
        return BindingBuilder.bind(sagaQueue())
                .to(itineraryEventsExchange())
                .with("itinerary.cancelled");
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }
}
