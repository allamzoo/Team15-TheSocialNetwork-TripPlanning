package com.team15.tripplanning.activityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;

import java.util.Collections;

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
        factory.setKeyspaceStartupScripts(Collections.singletonList(
                "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName
                        + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"
                        + " AND durable_writes = true"
        ));
        return factory;
    }
}
