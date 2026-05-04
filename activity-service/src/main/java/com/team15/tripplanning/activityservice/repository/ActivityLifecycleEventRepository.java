package com.team15.tripplanning.activityservice.repository;

import java.time.Instant;
import java.util.List;

/**
 * Documentation-only interface describing the Cassandra queries that back
 * ActivityLifecycleEventStore when cassandra.enabled=true.
 *
 * IMPORTANT: This interface intentionally does NOT extend CassandraRepository
 * and carries NO @Repository annotation. Extending CassandraRepository causes
 * DataCassandraRepositoriesRegistrar to attempt bean creation even when all
 * Cassandra autoconfiguration is excluded, crashing the service startup when
 * no Cassandra cluster is reachable.
 *
 * When Cassandra is enabled, create a @ConditionalOnProperty(cassandra.enabled=true)
 * @Service that wraps the real CassandraRepository and implements ActivityLifecycleEventStore.
 *
 * Equivalent CQL queries for reference:
 *   findByActivityId:
 *     SELECT * FROM activity_lifecycle_events WHERE activity_id = ?
 *   findByActivityIdAndEventTimestampBetween:
 *     SELECT * FROM activity_lifecycle_events WHERE activity_id = ?
 *       AND event_timestamp >= ? AND event_timestamp <= ?
 */
public interface ActivityLifecycleEventRepository {
    // intentionally empty — see ActivityLifecycleEventStore + NoOpActivityLifecycleEventStore
}
