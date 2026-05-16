package com.team15.tripplanning.activityservice.messaging.publisher;

import com.team15.tripplanning.activityservice.config.ActivityEventConfig;
import com.team15.tripplanning.contracts.events.ActivityCancelledEvent;
import com.team15.tripplanning.contracts.events.ActivityCreatedEvent;
import com.team15.tripplanning.contracts.events.ActivityLifecycleRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ActivityEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ActivityEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Published by S4-F2 and S4-F4 after a successful Activity row insert.
     */
    public void publishActivityCreated(Long activityId, Long itineraryId, String category) {
        ActivityCreatedEvent event = new ActivityCreatedEvent(activityId, itineraryId, category);
        MDC.put("routingKey", ActivityEventConfig.RK_ACTIVITY_CREATED);
        try {
            rabbitTemplate.convertAndSend(
                    ActivityEventConfig.ACTIVITY_EXCHANGE,
                    ActivityEventConfig.RK_ACTIVITY_CREATED,
                    event
            );
            log.info("Published {} for activityId={}", ActivityEventConfig.RK_ACTIVITY_CREATED, activityId);
        } finally {
            MDC.remove("routingKey");
        }
    }

    /**
     * Published by the itinerary.completed consumer — once per activity
     * whose COMPLETED Cassandra row was freshly inserted (not a duplicate).
     */
    public void publishActivityLifecycleRecorded(Long activityId, Long itineraryId, String status) {
        ActivityLifecycleRecordedEvent event =
                new ActivityLifecycleRecordedEvent(activityId, itineraryId, status);
        MDC.put("routingKey", ActivityEventConfig.RK_ACTIVITY_LIFECYCLE_RECORDED);
        try {
            rabbitTemplate.convertAndSend(
                    ActivityEventConfig.ACTIVITY_EXCHANGE,
                    ActivityEventConfig.RK_ACTIVITY_LIFECYCLE_RECORDED,
                    event
            );
            log.info("Published {} for activityId={} status={}",
                    ActivityEventConfig.RK_ACTIVITY_LIFECYCLE_RECORDED, activityId, status);
        } finally {
            MDC.remove("routingKey");
        }
    }

    /**
     * Published by the itinerary.cancelled consumer — once per activity
     * whose CANCELLED Cassandra row was freshly inserted (not a duplicate).
     */
    public void publishActivityCancelled(Long activityId, Long itineraryId) {
        ActivityCancelledEvent event = new ActivityCancelledEvent(activityId, itineraryId);
        MDC.put("routingKey", ActivityEventConfig.RK_ACTIVITY_CANCELLED);
        try {
            rabbitTemplate.convertAndSend(
                    ActivityEventConfig.ACTIVITY_EXCHANGE,
                    ActivityEventConfig.RK_ACTIVITY_CANCELLED,
                    event
            );
            log.info("Published {} for activityId={}", ActivityEventConfig.RK_ACTIVITY_CANCELLED, activityId);
        } finally {
            MDC.remove("routingKey");
        }
    }
}