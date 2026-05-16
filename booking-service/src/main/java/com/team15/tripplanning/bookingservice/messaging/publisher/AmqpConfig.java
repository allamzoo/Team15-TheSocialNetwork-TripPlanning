package com.team15.tripplanning.bookingservice.messaging.publisher;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

    public static final String PAYMENT_EXCHANGE = "payment.events";
    public static final String ITINERARY_EXCHANGE = "itinerary.events";
    public static final String SAGA_QUEUE = "payment.saga-listener";
    public static final String SAGA_DLQ = "payment.saga-listener.dlq";
    public static final String SAGA_DLX = "payment.saga-listener.dlx";
    public static final String SAGA_DLQ_ROUTING_KEY = "payment.saga-listener.dlq";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange itineraryExchange() {
        return new TopicExchange(ITINERARY_EXCHANGE);
    }

    @Bean
    public TopicExchange sagaDeadLetterExchange() {
        return new TopicExchange(SAGA_DLX);
    }

    @Bean
    public Queue sagaQueue() {
        return QueueBuilder.durable(SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_DLX)
                .withArgument("x-dead-letter-routing-key", SAGA_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue sagaDeadLetterQueue() {
        return QueueBuilder.durable(SAGA_DLQ).build();
    }

    @Bean
    public Binding itineraryCompletedBinding(Queue sagaQueue, TopicExchange itineraryExchange) {
        return BindingBuilder.bind(sagaQueue).to(itineraryExchange).with("itinerary.completed");
    }

    @Bean
    public Binding itineraryCancelledBinding(Queue sagaQueue, TopicExchange itineraryExchange) {
        return BindingBuilder.bind(sagaQueue).to(itineraryExchange).with("itinerary.cancelled");
    }

    @Bean
    public Binding sagaDeadLetterBinding(Queue sagaDeadLetterQueue, TopicExchange sagaDeadLetterExchange) {
        return BindingBuilder.bind(sagaDeadLetterQueue).to(sagaDeadLetterExchange).with(SAGA_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}

