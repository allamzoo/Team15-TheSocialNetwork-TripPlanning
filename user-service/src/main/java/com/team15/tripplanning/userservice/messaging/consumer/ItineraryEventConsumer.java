package com.team15.tripplanning.userservice.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.userservice.config.UserEventConfig;
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

    @RabbitListener(queues = UserEventConfig.SAGA_QUEUE)
    public void onItineraryEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        MDC.put("routingKey", routingKey);
        try {
            log.info("Consuming {} event", routingKey);
            if ("itinerary.completed".equals(routingKey)) {
                ItineraryCompletedEvent event = objectMapper.readValue(
                        message.getBody(), ItineraryCompletedEvent.class);
                MDC.put("itineraryId", String.valueOf(event.itineraryId()));
                MDC.put("userId", String.valueOf(event.userId()));
                invalidateTripCaches(event.userId());
                log.info("Processed {} for itineraryId={}", routingKey, event.itineraryId());

            } else if ("itinerary.cancelled".equals(routingKey)) {
                ItineraryCancelledEvent event = objectMapper.readValue(
                        message.getBody(), ItineraryCancelledEvent.class);
                MDC.put("itineraryId", String.valueOf(event.itineraryId()));
                MDC.put("userId", String.valueOf(event.userId()));
                invalidateTripCaches(event.userId());
                log.info("Processed {} for itineraryId={}", routingKey, event.itineraryId());
            }
        } catch (Exception e) {
            log.error("Failed to process {}: {}", routingKey, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("itineraryId");
            MDC.remove("userId");
        }
    }

    private void invalidateTripCaches(Long userId) {
        deleteWildcard("s1-f3-trip-summary::S1::S1-F3::" + userId);
        deleteWildcard("s1-f9-travel-style::*");
        log.info("Invalidated trip caches for userId={}", userId);
    }

    private void deleteWildcard(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
