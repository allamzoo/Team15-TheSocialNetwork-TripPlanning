package com.team15.tripplanning.activityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.Collections;

@Configuration
@EnableCassandraRepositories(basePackages = "com.team15.tripplanning.activityservice.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${spring.data.cassandra.keyspace-name:tripplanning}")
    private String keyspaceName;

    @Value("${spring.data.cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port:9042}")
    private int port;

    @Value("${spring.data.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    public String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[]{"com.team15.tripplanning.activityservice.model.cassandra"};
    }

    /**
     * Override to inject a keyspace startup script so the keyspace is created
     * before the session attempts to USE it.  The parent implementation of
     * cassandraSession() passes these to CqlSessionFactoryBean.setKeyspaceStartupScripts()
     * which runs them against the cluster (no keyspace selected) before the
     * regular session is opened.
     */
    @Override
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
