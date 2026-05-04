package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;

import java.util.List;

/**
 * Abstraction over the Cassandra lifecycle-event storage.
 * When cassandra.enabled=false, a no-op implementation is injected.
 * When cassandra.enabled=true, the real CassandraActivityLifecycleEventStore wraps
 * ActivityLifecycleEventRepository.
 */
public interface ActivityLifecycleEventStore {

    void save(ActivityLifecycleEvent event);

    /**
     * Returns all lifecycle events for the given activity, sorted by most recent first.
     * Used by the timeline endpoint (S4-F12).
     */
    List<ActivityLifecycleEvent> findByActivityId(Long activityId);

    /** Alias kept for internal consistency with the composite-key naming convention. */
    default List<ActivityLifecycleEvent> findByKeyActivityId(Long activityId) {
        return findByActivityId(activityId);
    }
}
