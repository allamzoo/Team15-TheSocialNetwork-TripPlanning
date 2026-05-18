package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true")
public class CassandraActivityLifecycleEventStore implements ActivityLifecycleEventStore {

    private final CassandraLifecycleEventCrudRepository repo;

    public CassandraActivityLifecycleEventStore(CassandraLifecycleEventCrudRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(ActivityLifecycleEvent event) {
        repo.save(event);
    }

    @Override
    public List<ActivityLifecycleEvent> findByActivityId(Long activityId) {
        return repo.findByKeyActivityId(activityId);
    }
    @Override
    public List<ActivityLifecycleEvent> findByActivityIdAndStatus(Long activityId, String status) {
        return repo.findByKeyActivityIdAndStatus(activityId, status);
    }
}
