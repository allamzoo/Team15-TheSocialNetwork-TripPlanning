package com.team15.tripplanning.itineraryservice.support;

import com.team15.tripplanning.contracts.feign.BookingServiceClient;
import com.team15.tripplanning.contracts.feign.DestinationServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for itinerary-service integration tests.
 *
 * Provides runtime independence:
 *
 * 1. Testcontainers RabbitMQ — broker started once, properties injected via @DynamicPropertySource.
 *
 * 2. @MockitoBean Feign clients — all three downstream clients are mocked.
 *    This service is the busiest Feign caller (S3-F2, S3-F4 pre-checks, S3-F11, S3-F12,
 *    plus all the new aggregate endpoints). Every test in this service can run in isolation
 *    without user-service, destination-service, or booking-service deployed.
 *
 * Saga tests: stub bookingServiceClient.getConfirmedSummary(id) to control whether
 * the saga trigger pre-check passes (count >= 1) or fails (count == 0).
 *
 * Usage:
 * <pre>
 *   class SagaTriggerTest extends BaseIntegrationTest {
 *       @Test
 *       void completeSetsCompletingStatus() {
 *           when(userServiceClient.getUser(1L))
 *               .thenReturn(new UserDTO(1L, "Ahmed", ..., "ACTIVE", ...));
 *           when(destinationServiceClient.getDestination(10L))
 *               .thenReturn(new DestinationDTO(10L, "Dahab", ..., "ACTIVE", ...));
 *           when(bookingServiceClient.getConfirmedSummary(1L))
 *               .thenReturn(new ConfirmedSummaryDTO(2, BigDecimal.valueOf(2000)));
 *           // ... PUT /api/itineraries/1/complete → expect 200, status=COMPLETING ...
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
     * Mocked UserServiceClient (S1).
     * S3-F4 pre-check: user.status must be ACTIVE before saga publish.
     * S3-F11, S3-F12: user name/preferences for Neo4j graph and recommendations.
     */
    @MockitoBean
    protected UserServiceClient userServiceClient;

    /**
     * Mocked DestinationServiceClient (S2).
     * S3-F2: destination must exist and be ACTIVE before assigning + publishing itinerary.placed.
     * S3-F4 pre-check: destination.status must be ACTIVE.
     * S3-F11, S3-F12: destination name/country/category for Neo4j graph and recommendations.
     */
    @MockitoBean
    protected DestinationServiceClient destinationServiceClient;

    /**
     * Mocked BookingServiceClient (S5).
     * S3-F4: getConfirmedSummary — count >= 1 check + totalRevenue → estimatedBudget.
     * S2-F3 revenue chain: aggregateByItineraries → DestinationBookingRevenueAggregateDTO.
     */
    @MockitoBean
    protected BookingServiceClient bookingServiceClient;
}
