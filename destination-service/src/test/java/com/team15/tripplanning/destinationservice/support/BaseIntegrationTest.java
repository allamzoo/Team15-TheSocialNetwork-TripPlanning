package com.team15.tripplanning.destinationservice.support;

import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import com.team15.tripplanning.destinationservice.config.TestInfrastructureConfig;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for destination-service integration tests.
 *
 * Provides runtime independence:
 *
 * 1. Testcontainers RabbitMQ — broker started once, host/port injected before context boots.
 *
 * 2. @MockitoBean Feign clients — ItineraryServiceClient and UserServiceClient are mocks.
 *    S2-F3 (revenue chain), S2-F4 (active-count), S2-F7 (itinerary validation),
 *    S2-F8 (ADMIN check), S2-F12 (dashboard aggregate) can all be tested without
 *    itinerary-service or user-service being deployed.
 *
 * Usage:
 * <pre>
 *   class DashboardTest extends BaseIntegrationTest {
 *       @Test
 *       void dashboardAggregatesViaFeign() {
 *           when(itineraryServiceClient.getDestinationDashboardAggregate(5L))
 *               .thenReturn(new DestinationDashboardAggregateDTO(5, 3, 3));
 *           // ... call GET /api/destinations/5/dashboard and assert ...
 *       }
 *   }
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestInfrastructureConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    static final RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:3.12-management")
                    .withReuse(true);

    @DynamicPropertySource
    static void rabbitMQProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host",     rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port",     rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    // ─── Feign mocks ─────────────────────────────────────────────────────────

    /**
     * Mocked ItineraryServiceClient (S3).
     * Used by S2-F3, S2-F4, S2-F7, S2-F12.
     */
    @MockitoBean
    protected ItineraryServiceClient itineraryServiceClient;

    /**
     * Mocked UserServiceClient (S1).
     * Used by S2-F8 (ADMIN role check — verifier must have role=ADMIN).
     */
    @MockitoBean
    protected UserServiceClient userServiceClient;
}
