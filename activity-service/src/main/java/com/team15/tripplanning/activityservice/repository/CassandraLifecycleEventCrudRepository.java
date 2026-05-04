package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEventKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true")
public interface CassandraLifecycleEventCrudRepository
        extends CassandraRepository<ActivityLifecycleEvent, ActivityLifecycleEventKey> {

    List<ActivityLifecycleEvent> findByKeyActivityId(Long activityId);
}
