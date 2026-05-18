package com.team15.tripplanning.itineraryservice.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team15.tripplanning.itineraryservice.config.ItineraryRabbitMQConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ItineraryCacheInvalidationConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ItineraryCacheInvalidationConsumer(RedisTemplate<String, Object> redisTemplate,
                                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = ItineraryRabbitMQConfig.USER_EVENTS_QUEUE)
    public void handleEvent(Message message,
                            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        switch (routingKey) {
            case "user.registered" -> {} // Approach 1 — no state to update on a brand-new user

            case "user.deactivated" -> {
                deleteWildcard("s3-itineraries::*");
                deleteWildcard("s3-analytics::*");
            }

            case "destination.status-changed" -> {
                deleteWildcard("s3-itineraries::*");
                deleteWildcard("s3-analytics::*");
            }

            case "destination.rated" ->
                deleteWildcard("s3-recommendations::*");

            case "activity.created", "activity.lifecycle-recorded", "activity.cancelled" -> {
                Long itineraryId = extractItineraryId(message);
                if (itineraryId != null) {
                    deleteWildcard("s3-details::S3::S3-F5::" + itineraryId);
                }
            }
        }
    }

    private Long extractItineraryId(Message message) {
        try {
            JsonNode node = objectMapper.readTree(message.getBody());
            JsonNode field = node.get("itineraryId");
            return (field != null && !field.isNull()) ? field.asLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteWildcard(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
