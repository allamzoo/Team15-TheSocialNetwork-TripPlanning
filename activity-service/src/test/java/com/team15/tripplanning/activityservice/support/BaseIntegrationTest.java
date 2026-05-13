package com.team15.tripplanning.activityservice.support;

import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for activity-service integration tests.
 *
 * Provides runtime independence:
 *
 * 1. Testcontainers RabbitMQ — broker started once per JVM, injected via @DynamicPropertySource.
 *
 * 2. @MockitoBean ItineraryServiceClient — activity-service's only outbound Feign call is
 *    the itinerary-existence check in S4-F2 and S4-F4. With this mock in place,
 *    activity-service tests run without itinerary-service deployed or its M3 endpoints
 *    implemented.
 *
 * Usage:
 * <pre>
 *   class ActivityCreationTest extends BaseIntegrationTest {
 *       @Test
 *       void createActivityChecksItineraryExists() {
 *           when(itineraryServiceClient.getItinerary(1L))
 *               .thenReturn(new ItineraryDTO(1L, 1L, 10L, "PLANNED", ...));
 *
 *           // POST /api/activities/itinerary/1 → 201
 *           // POST /api/activities/itinerary/999 → stub throws FeignException.NotFound → 404
 *       }
 *   }
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
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
     * Used by S4-F2 and S4-F4 for the itinerary-existence check before creating an Activity.
     *
     * To simulate a missing itinerary (→ 404 response):
     *   when(itineraryServiceClient.getItinerary(999L))
     *       .thenThrow(new FeignException.NotFound("not found", ...));
     */
    @MockitoBean
    protected ItineraryServiceClient itineraryServiceClient;
}
