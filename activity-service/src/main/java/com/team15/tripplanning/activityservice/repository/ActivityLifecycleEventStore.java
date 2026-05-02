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

    List<ActivityLifecycleEvent> findByKeyActivityId(Long activityId);
}
