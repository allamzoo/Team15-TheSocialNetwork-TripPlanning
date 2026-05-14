package com.team15.tripplanning.bookingservice.messaging.publisher;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

    // ─── Exchanges ────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange("payment.events", true, false);
    }

    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange("itinerary.events", true, false);
    }

    // ─── Queue: booking-service consumes itinerary.completed events ───────────

    @Bean
    public Queue itineraryBookingConsumerQueue() {
        return new Queue("itinerary.events.booking-consumer", true);
    }

    @Bean
    public Binding itineraryCompletedBinding(Queue itineraryBookingConsumerQueue,
                                              TopicExchange itineraryEventsExchange) {
        return BindingBuilder
                .bind(itineraryBookingConsumerQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.completed");
    }

    // ─── Queue: booking-service consumes itinerary.cancelled events ───────────

    @Bean
    public Queue itineraryCancelledBookingQueue() {
        return new Queue("itinerary.events.booking-cancelled-consumer", true);
    }

    @Bean
    public Binding itineraryCancelledBinding(Queue itineraryCancelledBookingQueue,
                                              TopicExchange itineraryEventsExchange) {
        return BindingBuilder
                .bind(itineraryCancelledBookingQueue)
                .to(itineraryEventsExchange)
                .with("itinerary.cancelled");
    }

    // ─── Jackson JSON message converter ──────────────────────────────────────

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
