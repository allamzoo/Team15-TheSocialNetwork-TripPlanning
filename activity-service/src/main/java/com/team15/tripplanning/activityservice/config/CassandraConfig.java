package com.team15.tripplanning.activityservice.config;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true")
public class CassandraConfig {

    @Value("${spring.data.cassandra.keyspace-name:tripplanning}")
    private String keyspaceName;

    @Value("${spring.data.cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port:9042}")
    private int port;

    @Value("${spring.data.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Bean
    public CqlSessionFactoryBean cassandraSession() {
        CqlSessionFactoryBean factory = new CqlSessionFactoryBean();
        factory.setContactPoints(contactPoints);
        factory.setPort(port);
        factory.setLocalDatacenter(localDatacenter);
        factory.setKeyspaceName(keyspaceName);

        // Increase driver request timeout to 30s for CREATE KEYSPACE / CREATE TABLE
        // startup scripts (default is 2s which can timeout on first-run or slow Cassandra).
        factory.setSessionBuilderConfigurer(sessionBuilder ->
                sessionBuilder.withConfigLoader(
                        DriverConfigLoader.programmaticBuilder()
                                .withDuration(DefaultDriverOption.REQUEST_TIMEOUT,
                                        Duration.ofSeconds(30))
                                .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT,
                                        Duration.ofSeconds(30))
                                .build()
                )
        );

        factory.setKeyspaceStartupScripts(java.util.Arrays.asList(
                "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName
                        + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"
                        + " AND durable_writes = true",
                "CREATE TABLE IF NOT EXISTS " + keyspaceName + ".activity_lifecycle_events ("
                        + "    activity_id bigint,"
                        + "    event_timestamp timestamp,"
                        + "    status text,"
                        + "    category text,"
                        + "    latitude double,"
                        + "    longitude double,"
                        + "    notes text,"
                        + "    PRIMARY KEY (activity_id, event_timestamp)"
                        + ") WITH CLUSTERING ORDER BY (event_timestamp DESC)"
        ));
        return factory;
    }
}
