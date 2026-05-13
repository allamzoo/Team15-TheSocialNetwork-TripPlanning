package com.team15.tripplanning.bookingservice.support;

import com.team15.tripplanning.contracts.feign.DestinationServiceClient;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
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
 * Base class for booking-service integration tests.
 *
 * Provides runtime independence:
 *
 * 1. Testcontainers RabbitMQ — broker started once per JVM, injected via @DynamicPropertySource.
 *    The payment.saga-listener queue and payment.events exchange can be verified in tests
 *    using real AMQP message delivery against this container.
 *
 * 2. @MockitoBean Feign clients — all three outbound clients are mocked.
 *    - S5-F3: user existence check (UserServiceClient)
 *    - S5-F4: itinerary status validation + active-count for seasonalSurcharge (ItineraryServiceClient)
 *    - S5-F10: batch itinerary + destination lookups (ItineraryServiceClient + DestinationServiceClient)
 *    - S5-F12: itinerary status + startDate for refund strategy (ItineraryServiceClient)
 *    None of those services need to be running for any booking-service test to pass.
 *
 * Usage:
 * <pre>
 *   class RefundStrategyTest extends BaseIntegrationTest {
 *       @Test
 *       void earlyRefundReturns100Percent() {
 *           when(itineraryServiceClient.getItinerary(10L))
 *               .thenReturn(new ItineraryDTO(10L, 1L, 5L, "PLANNED",
 *                   LocalDate.now().plusDays(30), null, null, null));
 *           // POST /api/bookings/100/refund-cancellation-tier → 200, refund=2000
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
     * S5-F3: user-existence check before returning booking summary.
     * S5 saga consumer: user details when creating SETTLEMENT_PENDING audit row.
     */
    @MockitoBean
    protected UserServiceClient userServiceClient;

    /**
     * Mocked ItineraryServiceClient (S3).
     * S5-F4: status validation (must be PLANNED or IN_PROGRESS) + active-count for surcharge.
     * S5-F10: batch itinerary → destinationId lookup.
     * S5-F12: itinerary status + startDate → refund strategy selection.
     */
    @MockitoBean
    protected ItineraryServiceClient itineraryServiceClient;

    /**
     * Mocked DestinationServiceClient (S2).
     * S5-F10: batch destination → name lookup for revenue grouping.
     */
    @MockitoBean
    protected DestinationServiceClient destinationServiceClient;
}
