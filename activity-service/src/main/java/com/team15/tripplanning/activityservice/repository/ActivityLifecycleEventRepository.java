package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEventKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLifecycleEventRepository
        extends CassandraRepository<ActivityLifecycleEvent, ActivityLifecycleEventKey> {

    List<ActivityLifecycleEvent> findByKeyActivityId(Long activityId);
}
