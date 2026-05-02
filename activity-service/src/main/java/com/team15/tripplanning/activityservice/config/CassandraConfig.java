package com.team15.tripplanning.activityservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Cassandra is NOT actively used by activity-service at runtime.
 * ActivityLogRow is a plain POJO used only in CassandraRowAdapter (GoF Adapter pattern).
 * No CassandraRepository or CassandraTemplate beans are needed.
 *
 * This config is intentionally disabled via @ConditionalOnProperty.
 * To enable real Cassandra connectivity, set cassandra.enabled=true
 * and ensure a reachable Cassandra cluster is configured.
 */
@Configuration
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true", matchIfMissing = false)
public class CassandraConfig {
    // intentionally empty — Cassandra is a soft adapter dependency only
}
