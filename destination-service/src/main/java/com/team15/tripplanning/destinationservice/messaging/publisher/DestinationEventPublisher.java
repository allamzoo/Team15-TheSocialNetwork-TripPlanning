package com.team15.tripplanning.destinationservice.messaging.publisher;

import com.team15.tripplanning.contracts.events.DestinationStatusChangedEvent;
import com.team15.tripplanning.contracts.events.DestinationRatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DestinationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DestinationEventPublisher.class);
    private static final String EXCHANGE = "destination.events";

    private final RabbitTemplate rabbitTemplate;

    public DestinationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishStatusChanged(Long destinationId, String oldStatus, String newStatus) {
        var event = new DestinationStatusChangedEvent(destinationId, oldStatus, newStatus);
        rabbitTemplate.convertAndSend(EXCHANGE, "destination.status-changed", event);
        log.info("Published destination.status-changed for destination={}", destinationId);
    }

    public void publishRated(Long destinationId, Long itineraryId, Double rating, Long userId) {
        var event = new DestinationRatedEvent(destinationId, itineraryId, rating, userId);
        rabbitTemplate.convertAndSend(EXCHANGE, "destination.rated", event);
        log.info("Published destination.rated for destination={}", destinationId);
    }
}