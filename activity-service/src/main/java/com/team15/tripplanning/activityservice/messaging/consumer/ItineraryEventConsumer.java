package com.team15.tripplanning.activityservice.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team15.tripplanning.activityservice.config.ActivityEventConfig;
import com.team15.tripplanning.activityservice.messaging.publisher.ActivityEventPublisher;
import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEventKey;
import com.team15.tripplanning.activityservice.repository.ActivityLifecycleEventStore;
import com.team15.tripplanning.activityservice.repository.ActivityRepository;
import com.team15.tripplanning.contracts.events.ItineraryCancelledEvent;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.contracts.events.ItineraryPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ItineraryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventConsumer.class);

    private final ActivityRepository activityRepository;
    private final ActivityLifecycleEventStore lifecycleEventStore;
    private final ActivityEventPublisher publisher;
    private final ObjectMapper objectMapper;

    public ItineraryEventConsumer(ActivityRepository activityRepository,
                                  ActivityLifecycleEventStore lifecycleEventStore,
                                  ActivityEventPublisher publisher,
                                  ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.lifecycleEventStore = lifecycleEventStore;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Single listener on the queue — all 3 routing keys land here.
     * We dispatch manually using the amqp_receivedRoutingKey header.
     */
    @RabbitListener(queues = ActivityEventConfig.ITINERARY_SAGA_QUEUE)
    public void onItineraryEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        MDC.put("routingKey", routingKey != null ? routingKey : "unknown");
        try {
            if (ActivityEventConfig.RK_ITINERARY_COMPLETED.equals(routingKey)) {
                ItineraryCompletedEvent event = objectMapper.readValue(
                        message.getBody(), ItineraryCompletedEvent.class);
                handleItineraryCompleted(event, routingKey);

            } else if (ActivityEventConfig.RK_ITINERARY_CANCELLED.equals(routingKey)) {
                ItineraryCancelledEvent event = objectMapper.readValue(
                        message.getBody(), ItineraryCancelledEvent.class);
                handleItineraryCancelled(event, routingKey);

            } else if (ActivityEventConfig.RK_ITINERARY_PLACED.equals(routingKey)) {
                ItineraryPlacedEvent event = objectMapper.readValue(
                        message.getBody(), ItineraryPlacedEvent.class);
                handleItineraryPlaced(event, routingKey);

            } else {
                log.warn("Received unknown routing key '{}' on {} — ignoring",
                        routingKey, ActivityEventConfig.ITINERARY_SAGA_QUEUE);
            }
        } catch (Exception e) {
            log.error("Failed to process {}: {}", routingKey, e.getMessage(), e);
            // Re-throw so Spring AMQP retry kicks in → DLQ after max-attempts
            throw new RuntimeException("Consumer failed for routingKey=" + routingKey, e);
        } finally {
            MDC.remove("routingKey");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // itinerary.placed — optional bookkeeping (SCHEDULED Cassandra row)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleItineraryPlaced(ItineraryPlacedEvent event, String routingKey) {
        MDC.put("itineraryId", String.valueOf(event.itineraryId()));
        try {
            log.info("Consuming {} for itineraryId={}", routingKey, event.itineraryId());

            List<Activity> activities =
                    activityRepository.findByItineraryId(event.itineraryId());

            for (Activity activity : activities) {
                List<ActivityLifecycleEvent> existing =
                        lifecycleEventStore.findByActivityIdAndStatus(
                                activity.getId(), "SCHEDULED");
                if (!existing.isEmpty()) {
                    log.info("Skipping duplicate SCHEDULED row for activityId={}",
                            activity.getId());
                    continue;
                }
                ActivityLifecycleEventKey key =
                        new ActivityLifecycleEventKey(activity.getId(), Instant.now());
                ActivityLifecycleEvent lifecycleEvent = new ActivityLifecycleEvent();
                lifecycleEvent.setKey(key);
                lifecycleEvent.setStatus("SCHEDULED");
                lifecycleEvent.setCategory(activity.getCategory().name());
                lifecycleEvent.setLatitude(activity.getLatitude());
                lifecycleEvent.setLongitude(activity.getLongitude());
                lifecycleEventStore.save(lifecycleEvent);
                log.info("Processed {} — SCHEDULED row written for activityId={}",
                        routingKey, activity.getId());
            }
        } finally {
            MDC.remove("itineraryId");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // itinerary.completed — write COMPLETED Cassandra row + publish lifecycle
    // ─────────────────────────────────────────────────────────────────────────
    private void handleItineraryCompleted(ItineraryCompletedEvent event, String routingKey) {
        MDC.put("itineraryId", String.valueOf(event.itineraryId()));
        try {
            log.info("Consuming {} for itineraryId={}", routingKey, event.itineraryId());

            List<Activity> activities =
                    activityRepository.findByItineraryId(event.itineraryId());

            for (Activity activity : activities) {
                MDC.put("activityId", String.valueOf(activity.getId()));
                try {
                    // Idempotency — check before insert
                    List<ActivityLifecycleEvent> existing =
                            lifecycleEventStore.findByActivityIdAndStatus(
                                    activity.getId(), "COMPLETED");
                    if (!existing.isEmpty()) {
                        log.info("Duplicate consume — COMPLETED row already exists " +
                                "for activityId={}", activity.getId());
                        continue;
                    }

                    ActivityLifecycleEventKey key =
                            new ActivityLifecycleEventKey(activity.getId(), Instant.now());
                    ActivityLifecycleEvent lifecycleEvent = new ActivityLifecycleEvent();
                    lifecycleEvent.setKey(key);
                    lifecycleEvent.setStatus("COMPLETED");
                    lifecycleEvent.setCategory(activity.getCategory().name());
                    lifecycleEvent.setLatitude(activity.getLatitude());
                    lifecycleEvent.setLongitude(activity.getLongitude());
                    lifecycleEventStore.save(lifecycleEvent);

                    log.info("DB write: activityId={} saved with status=COMPLETED",
                            activity.getId());

                    publisher.publishActivityLifecycleRecorded(
                            activity.getId(), event.itineraryId(), "COMPLETED");

                } finally {
                    MDC.remove("activityId");
                }
            }
            log.info("Processed {} for itineraryId={}", routingKey, event.itineraryId());
        } finally {
            MDC.remove("itineraryId");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // itinerary.cancelled — write CANCELLED Cassandra row + publish cancelled
    // ─────────────────────────────────────────────────────────────────────────
    private void handleItineraryCancelled(ItineraryCancelledEvent event, String routingKey) {
        MDC.put("itineraryId", String.valueOf(event.itineraryId()));
        try {
            log.info("Consuming {} for itineraryId={}", routingKey, event.itineraryId());

            List<Activity> activities =
                    activityRepository.findByItineraryId(event.itineraryId());

            for (Activity activity : activities) {
                MDC.put("activityId", String.valueOf(activity.getId()));
                try {
                    // Idempotency — check before insert
                    List<ActivityLifecycleEvent> existing =
                            lifecycleEventStore.findByActivityIdAndStatus(
                                    activity.getId(), "CANCELLED");
                    if (!existing.isEmpty()) {
                        log.info("Duplicate consume — CANCELLED row already exists " +
                                "for activityId={}", activity.getId());
                        continue;
                    }

                    ActivityLifecycleEventKey key =
                            new ActivityLifecycleEventKey(activity.getId(), Instant.now());
                    ActivityLifecycleEvent lifecycleEvent = new ActivityLifecycleEvent();
                    lifecycleEvent.setKey(key);
                    lifecycleEvent.setStatus("CANCELLED");
                    lifecycleEvent.setCategory(activity.getCategory().name());
                    lifecycleEvent.setLatitude(activity.getLatitude());
                    lifecycleEvent.setLongitude(activity.getLongitude());
                    lifecycleEventStore.save(lifecycleEvent);

                    log.info("DB write: activityId={} saved with status=CANCELLED",
                            activity.getId());

                    publisher.publishActivityCancelled(
                            activity.getId(), event.itineraryId());

                } finally {
                    MDC.remove("activityId");
                }
            }
            log.info("Processed {} for itineraryId={}", routingKey, event.itineraryId());
        } finally {
            MDC.remove("itineraryId");
        }
    }
}