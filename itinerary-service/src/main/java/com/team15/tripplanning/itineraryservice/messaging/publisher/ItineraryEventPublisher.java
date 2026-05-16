package com.team15.tripplanning.itineraryservice.messaging.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.contracts.events.ItineraryPlacedEvent;
import com.team15.tripplanning.itineraryservice.config.ItineraryRabbitMQConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ItineraryEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public ItineraryEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishPlaced(ItineraryPlacedEvent event) {
        send("itinerary.placed", event);
    }

    public void publishCompleted(ItineraryCompletedEvent event) {
        send("itinerary.completed", event);
    }

    public void publishCancelled(ItineraryCancelledEvent event) {
        send("itinerary.cancelled", event);
    }

    private void send(String routingKey, Object event) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(event);
            Message message = MessageBuilder.withBody(body)
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            rabbitTemplate.send(ItineraryRabbitMQConfig.ITINERARY_EXCHANGE, routingKey, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event with routing key: " + routingKey, e);
        }
    }
}
