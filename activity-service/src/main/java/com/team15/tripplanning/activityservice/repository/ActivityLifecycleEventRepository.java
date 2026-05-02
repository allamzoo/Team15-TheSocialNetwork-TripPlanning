package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEventKey;
import java.time.Instant;
import java.util.List;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLifecycleEventRepository
        extends CassandraRepository<ActivityLifecycleEvent, ActivityLifecycleEventKey> {

    @Query("SELECT * FROM activity_lifecycle_events WHERE activity_id = ?0")
    List<ActivityLifecycleEvent> findByActivityId(Long activityId);

    @Query("SELECT * FROM activity_lifecycle_events WHERE activity_id = ?0 AND event_timestamp >= ?1 AND event_timestamp <= ?2")
    List<ActivityLifecycleEvent> findByActivityIdAndEventTimestampBetween(
            Long activityId,
            Instant start,
            Instant end
    );
}