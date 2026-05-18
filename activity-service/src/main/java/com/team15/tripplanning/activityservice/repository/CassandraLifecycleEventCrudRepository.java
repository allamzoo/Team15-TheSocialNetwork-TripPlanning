package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEventKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.cassandra.repository.Query;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true")
public interface CassandraLifecycleEventCrudRepository
        extends CassandraRepository<ActivityLifecycleEvent, ActivityLifecycleEventKey> {

    List<ActivityLifecycleEvent> findByKeyActivityId(Long activityId);

    /**
     * Used for idempotency check — find all rows for this activity with a given status.
     * Because status is NOT in the primary key, this is a ALLOW FILTERING query.
     * Safe here because activityId is the partition key (small result set per partition).
     */
    @Query("SELECT * FROM activity_lifecycle_events WHERE activity_id = ?0 AND status = ?1 ALLOW FILTERING")
    List<ActivityLifecycleEvent> findByKeyActivityIdAndStatus(Long activityId, String status);
}
