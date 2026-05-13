package com.team15.tripplanning.userservice.support;

import com.team15.tripplanning.contracts.feign.BookingServiceClient;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.userservice.config.TestInfrastructureConfig;
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
 * Base class for user-service integration tests.
 *
 * Provides runtime independence in two ways:
 *
 * 1. Testcontainers RabbitMQ — a real broker is started once per JVM (static @Container)
 *    and its host/port are injected via @DynamicPropertySource before the Spring context
 *    boots. This prevents AMQP auto-configuration from trying to reach the non-existent
 *    docker-compose rabbitmq service during test runs.
 *
 * 2. @MockitoBean Feign clients — ItineraryServiceClient and BookingServiceClient are replaced
 *    with Mockito mocks in the Spring context. Tests can stub responses with when(...) /
 *    thenReturn(...) without itinerary-service or booking-service being deployed or even
 *    having their M3 endpoints implemented yet.
 *
 * Usage:
 * <pre>
 *   class MyFeatureTest extends BaseIntegrationTest {
 *
 *       @Autowired TestRestTemplate restTemplate;
 *
 *       @Test
 *       void tripSummaryUsesFeign() {
 *           when(itineraryServiceClient.getUserItinerarySummary(1L))
 *               .thenReturn(new UserTripSummaryAggregateDTO(5, 3, 1,
 *                   BigDecimal.valueOf(2250), BigDecimal.valueOf(750)));
 *
 *           ResponseEntity<String> resp = restTemplate.getForEntity("/api/users/1/trip-summary", String.class);
 *           assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
 *       }
 *   }
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestInfrastructureConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    /**
     * Single RabbitMQ container shared across all tests in this JVM.
     * withReuse(true) means the container is NOT stopped between test classes —
     * Docker pulls once, amortised across the entire test suite.
     */
    @Container
    static final RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:3.12-management")
                    .withReuse(true);

    /**
     * Overrides spring.rabbitmq.* before the Spring context starts.
     * Without this, AMQP auto-configuration tries localhost:5672 and fails in CI.
     */
    @DynamicPropertySource
    static void rabbitMQProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host",     rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port",     rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    // ─── Feign mocks — no itinerary-service or booking-service needed ─────────

    /**
     * Mocked ItineraryServiceClient (S3).
     * Stub with: when(itineraryServiceClient.getUserItinerarySummary(userId)).thenReturn(...)
     */
    @MockitoBean
    protected ItineraryServiceClient itineraryServiceClient;

    /**
     * Mocked BookingServiceClient (S5).
     * Stub with: when(bookingServiceClient.getUserBookingTotal(userId, start, end)).thenReturn(...)
     */
    @MockitoBean
    protected BookingServiceClient bookingServiceClient;
}
