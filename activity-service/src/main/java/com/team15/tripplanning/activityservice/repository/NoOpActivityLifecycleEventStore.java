package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * No-op implementation of ActivityLifecycleEventStore used when cassandra.enabled=false.
 * Cassandra is a soft dependency — its absence must never crash the service.
 * Lifecycle events are still observed via MongoDB (MongoEventLogger) through the
 * notifyObservers("EVENT_RECORDED", ...) call in ActivityService.
 */
@Component
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpActivityLifecycleEventStore implements ActivityLifecycleEventStore {

    private static final Logger log = LoggerFactory.getLogger(NoOpActivityLifecycleEventStore.class);

    @Override
    public void save(ActivityLifecycleEvent event) {
        log.warn("Cassandra disabled — lifecycle event skipped for activityId={}",
                event.getKey() != null ? event.getKey().getActivityId() : "unknown");
    }

    @Override
    public List<ActivityLifecycleEvent> findByActivityId(Long activityId) {
        log.warn("Cassandra disabled — returning empty timeline for activityId={}", activityId);
        return List.of();
    }
}
