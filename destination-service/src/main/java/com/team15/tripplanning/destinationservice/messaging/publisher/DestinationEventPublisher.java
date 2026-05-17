package com.team15.tripplanning.destinationservice.messaging.publisher;

import com.team15.tripplanning.contracts.events.DestinationRatedEvent;
import com.team15.tripplanning.contracts.events.DestinationStatusChangedEvent;
import com.team15.tripplanning.destinationservice.config.DestinationRabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DestinationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DestinationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public DestinationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** S2-F4: published after every destination status transition. */
    public void publishStatusChanged(Long destinationId, String oldStatus, String newStatus) {
        MDC.put("destinationId", destinationId.toString());
        MDC.put("routingKey", DestinationRabbitMQConfig.ROUTING_KEY_STATUS_CHANGED);
        try {
            DestinationStatusChangedEvent event =
                    new DestinationStatusChangedEvent(destinationId, oldStatus, newStatus);
            rabbitTemplate.convertAndSend(
                    DestinationRabbitMQConfig.DESTINATION_EVENTS_EXCHANGE,
                    DestinationRabbitMQConfig.ROUTING_KEY_STATUS_CHANGED,
                    event
            );
            log.info("Published {} for destinationId={}", DestinationRabbitMQConfig.ROUTING_KEY_STATUS_CHANGED, destinationId);
        } catch (Exception e) {
            log.warn("Failed to publish {}: {}", DestinationRabbitMQConfig.ROUTING_KEY_STATUS_CHANGED, e.getMessage());
        } finally {
            MDC.remove("routingKey");
            MDC.remove("destinationId");
        }
    }

    /** S2-F7: published after a user successfully rates a destination. */
    public void publishRated(Long destinationId, Long itineraryId, Double rating, Long userId) {
        MDC.put("destinationId", destinationId.toString());
        MDC.put("itineraryId", itineraryId.toString());
        MDC.put("routingKey", DestinationRabbitMQConfig.ROUTING_KEY_RATED);
        try {
            DestinationRatedEvent event =
                    new DestinationRatedEvent(destinationId, itineraryId, rating, userId);
            rabbitTemplate.convertAndSend(
                    DestinationRabbitMQConfig.DESTINATION_EVENTS_EXCHANGE,
                    DestinationRabbitMQConfig.ROUTING_KEY_RATED,
                    event
            );
            log.info("Published {} for destinationId={}", DestinationRabbitMQConfig.ROUTING_KEY_RATED, destinationId);
        } catch (Exception e) {
            log.warn("Failed to publish {}: {}", DestinationRabbitMQConfig.ROUTING_KEY_RATED, e.getMessage());
        } finally {
            MDC.remove("routingKey");
            MDC.remove("itineraryId");
            MDC.remove("destinationId");
        }
    }
}
