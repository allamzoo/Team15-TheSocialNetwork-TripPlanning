package com.team15.tripplanning.userservice.messaging.publisher;

import com.team15.tripplanning.contracts.events.UserDeactivatedEvent;
import com.team15.tripplanning.contracts.events.UserRegisteredEvent;
import com.team15.tripplanning.userservice.config.UserEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private static final String ROUTING_KEY_REGISTERED  = "user.registered";
    private static final String ROUTING_KEY_DEACTIVATED = "user.deactivated";

    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(Long userId, String email, String role) {
        UserRegisteredEvent event = new UserRegisteredEvent(userId, email, role);
        log.info("Published {} for userId={}", ROUTING_KEY_REGISTERED, userId);
        rabbitTemplate.convertAndSend(UserEventConfig.USER_EVENTS_EXCHANGE, ROUTING_KEY_REGISTERED, event);
    }

    public void publishUserDeactivated(Long userId) {
        UserDeactivatedEvent event = new UserDeactivatedEvent(userId);
        log.info("Published {} for userId={}", ROUTING_KEY_DEACTIVATED, userId);
        rabbitTemplate.convertAndSend(UserEventConfig.USER_EVENTS_EXCHANGE, ROUTING_KEY_DEACTIVATED, event);
    }
}
