package com.team15.tripplanning.activityservice.repository;

/**
 * Marker interface kept for documentation purposes.
 *
 * When cassandra.enabled=true, provide a real implementation of ActivityLifecycleEventStore
 * backed by spring-data-cassandra (CassandraRepository). For now, Cassandra is a soft
 * dependency and NoOpActivityLifecycleEventStore is used instead.
 *
 * IMPORTANT: This interface intentionally does NOT extend CassandraRepository.
 * Extending CassandraRepository causes DataCassandraRepositoriesRegistrar to try
 * to create a Cassandra bean even when all Cassandra autoconfiguration is excluded,
 * crashing the service when no Cassandra cluster is reachable.
 */
public interface ActivityLifecycleEventRepository {
    // intentionally empty — see ActivityLifecycleEventStore + NoOpActivityLifecycleEventStore
}
