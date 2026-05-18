package com.team15.tripplanning.destinationservice.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.contracts.events.ItineraryPlacedEvent;
import com.team15.tripplanning.destinationservice.config.DestinationRabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ItineraryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventConsumer.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ItineraryEventConsumer(RedisTemplate<String, Object> redisTemplate,
                                  ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Single entry point for all itinerary lifecycle events on this queue.
     * Dispatches by routing key so each message is processed exactly once
     * regardless of its payload type — avoids deserialization failures when
     * multiple event types share one queue.
     */
    @RabbitListener(queues = DestinationRabbitMQConfig.ITINERARY_SAGA_QUEUE)
    public void handleItineraryEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        MDC.put("routingKey", routingKey != null ? routingKey : "unknown");
        try {
            switch (routingKey != null ? routingKey : "") {
                case "itinerary.placed"    -> handlePlaced(message);
                case "itinerary.completed" -> handleCompleted(message);
                case "itinerary.cancelled" -> handleCancelled(message);
                default -> log.warn("Received unknown routing key on saga queue: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process {}: {}", routingKey, e.getMessage());
            throw new RuntimeException(e); // nack → DLQ
        } finally {
            MDC.remove("routingKey");
            MDC.remove("itineraryId");
            MDC.remove("destinationId");
        }
    }

    private void handlePlaced(Message message) throws Exception {
        ItineraryPlacedEvent event = objectMapper.readValue(message.getBody(), ItineraryPlacedEvent.class);
        MDC.put("itineraryId", String.valueOf(event.itineraryId()));
        MDC.put("destinationId", String.valueOf(event.destinationId()));
        log.info("Consuming itinerary.placed for itineraryId={}", event.itineraryId());
        invalidateDestinationCaches(event.destinationId());
        log.info("Processed itinerary.placed for itineraryId={}", event.itineraryId());
    }

    private void handleCompleted(Message message) throws Exception {
        ItineraryCompletedEvent event = objectMapper.readValue(message.getBody(), ItineraryCompletedEvent.class);
        MDC.put("itineraryId", String.valueOf(event.itineraryId()));
        MDC.put("destinationId", String.valueOf(event.destinationId()));
        log.info("Consuming itinerary.completed for itineraryId={}", event.itineraryId());
        invalidateDestinationCaches(event.destinationId());
        log.info("Processed itinerary.completed for itineraryId={}", event.itineraryId());
    }

    private void handleCancelled(Message message) throws Exception {
        ItineraryCancelledEvent event = objectMapper.readValue(message.getBody(), ItineraryCancelledEvent.class);
        MDC.put("itineraryId", String.valueOf(event.itineraryId()));
        MDC.put("destinationId", String.valueOf(event.destinationId()));
        log.info("Consuming itinerary.cancelled for itineraryId={}", event.itineraryId());
        invalidateDestinationCaches(event.destinationId());
        log.info("Processed itinerary.cancelled for itineraryId={}", event.itineraryId());
    }

    /**
     * Wildcard-invalidates S2-F3 revenue and S2-F12 dashboard caches for this destination.
     * Next read recomputes via Feign. Replay is harmless (idempotent eviction).
     */
    private void invalidateDestinationCaches(Long destinationId) {
        deleteWildcard("s2-dest-revenue::S2::S2-F3::" + destinationId + "*");
        deleteWildcard("destination-service::S2-F12::" + destinationId);
    }

    private void deleteWildcard(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cache key(s) matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Cache eviction skipped (Redis unavailable) for pattern {}: {}", pattern, e.getMessage());
        }
    }
}
