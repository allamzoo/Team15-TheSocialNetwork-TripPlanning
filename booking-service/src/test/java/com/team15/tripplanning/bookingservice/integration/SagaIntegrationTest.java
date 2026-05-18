package com.team15.tripplanning.bookingservice.integration;

import com.team15.tripplanning.bookingservice.feature.aggregate.BookingAggregateService;
import com.team15.tripplanning.bookingservice.messaging.consumer.ItineraryEventConsumer;
import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import com.team15.tripplanning.bookingservice.saga.SettlementSaga;
import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import com.team15.tripplanning.contracts.feign.DestinationServiceClient;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S5-INFRA — Saga end-to-end integration tests (§8.6 Scenarios A, B, C).
 *
 * Uses webEnvironment=NONE — no embedded Tomcat — so tests run on Windows
 * without the WEPollSelectorImpl loopback issue (same root cause as
 * CachingIntegrationTest in user-service).  The saga logic is exercised
 * directly through service beans, which is the correct level for testing:
 *   - ItineraryEventConsumer.onItineraryCompleted()  (real @RabbitListener path)
 *   - SettlementSaga.process() / fail()              (state machine)
 *   - BookingAggregateService.getConfirmedSummary()  (pre-check)
 *   - RabbitTemplate.receiveAndConvert()             (event assertions)
 *
 * Infrastructure:
 *   - RabbitMQ : real Testcontainers broker (own @Container, no withReuse)
 *   - Database : H2 in-memory (no external Postgres)
 *   - Feign    : @MockitoBean (no external services)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "spring.datasource.url=jdbc:h2:mem:sagatest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
                "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SagaIntegrationTest {

    // ── Testcontainers RabbitMQ — no withReuse ────────────────────────────────
    @Container
    static final RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host",     rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port",     rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    // ── Feign mocks ───────────────────────────────────────────────────────────
    @MockitoBean UserServiceClient        userServiceClient;
    @MockitoBean ItineraryServiceClient   itineraryServiceClient;
    @MockitoBean DestinationServiceClient destinationServiceClient;

    // ── Beans under test ──────────────────────────────────────────────────────
    @Autowired private ItineraryEventConsumer  itineraryEventConsumer;
    @Autowired private SettlementSaga          settlementSaga;
    @Autowired private BookingAggregateService bookingAggregateService;
    @Autowired private RabbitTemplate          rabbitTemplate;
    @Autowired private RabbitAdmin             rabbitAdmin;
    @Autowired private BookingRepository       bookingRepository;
    @Autowired private SettlementRepository    settlementRepository;
    @Autowired private JdbcTemplate            jdbcTemplate;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String PAYMENT_EXCHANGE = "payment.events";

    private static final long       ITINERARY_ID_AB = 20L;
    private static final long       ITINERARY_ID_C  = 21L;
    private static final long       USER_ID         = 1L;
    private static final BigDecimal AMOUNT          = BigDecimal.valueOf(2000);

    // ── Setup ─────────────────────────────────────────────────────────────────
    @BeforeEach
    void resetData() {
        settlementRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    // ── Helper: declare a temporary auto-delete queue on payment.events ───────
    private String declarePaymentListenerQueue(String routingKey) {
        String name = "test.listener." + routingKey + "." + System.currentTimeMillis();
        rabbitAdmin.declareQueue(new Queue(name, false, false, true));
        rabbitAdmin.declareBinding(new Binding(
                name, Binding.DestinationType.QUEUE,
                PAYMENT_EXCHANGE, routingKey, null));
        return name;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario A — Happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("A) ItineraryCompletedEvent → SETTLEMENT_PENDING → process → SETTLED + payment.completed published")
    void scenarioA_happyPath() {
        // Arrange: subscribe to payment.completed before triggering saga
        String completedQueue = declarePaymentListenerQueue("payment.completed");

        // Act 1: consumer receives ItineraryCompletedEvent and creates SETTLEMENT_PENDING
        var event = new ItineraryCompletedEvent(ITINERARY_ID_AB, USER_ID, 10L, AMOUNT);
        itineraryEventConsumer.onItineraryCompleted(event);

        // Assert 1: SETTLEMENT_PENDING row created
        var s = settlementRepository.findByItineraryId(ITINERARY_ID_AB);
        assertThat(s).isPresent();
        assertThat(s.get().getStatus()).isEqualTo(Settlement.SettlementStatus.PENDING);

        // Act 2: saga processes the settlement (valid amount + matching userId)
        var settled = settlementSaga.process(ITINERARY_ID_AB, USER_ID, AMOUNT);

        // Assert 2: Settlement is now SETTLED with a settledAt timestamp
        assertThat(settled.getStatus()).isEqualTo(Settlement.SettlementStatus.COMPLETED);
        assertThat(settled.getSettledAt()).isNotNull();

        // Assert 3: payment.completed event published to RabbitMQ
        Awaitility.await("payment.completed published")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    Object received = rabbitTemplate.receiveAndConvert(completedQueue);
                    assertThat(received)
                            .as("Expected PaymentCompletedEvent on payment.completed queue")
                            .isNotNull()
                            .isInstanceOf(PaymentCompletedEvent.class);
                    PaymentCompletedEvent pce = (PaymentCompletedEvent) received;
                    assertThat(pce.itineraryId()).isEqualTo(ITINERARY_ID_AB);
                    assertThat(pce.amount()).isEqualByComparingTo(AMOUNT);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario B — Payment failure (amount = 0)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("B) process(amount=0) → PAYMENT_FAILED + payment.failed published")
    void scenarioB_paymentFailure() {
        // Arrange: subscribe to payment.failed before triggering saga
        String failedQueue = declarePaymentListenerQueue("payment.failed");

        // Act 1: reach SETTLEMENT_PENDING via the consumer
        itineraryEventConsumer.onItineraryCompleted(
                new ItineraryCompletedEvent(ITINERARY_ID_AB, USER_ID, 10L, AMOUNT));

        assertThat(settlementRepository.findByItineraryId(ITINERARY_ID_AB)).isPresent();

        // Act 2: process with amount=0 (deliberately invalid → saga fails)
        var failed = settlementSaga.process(ITINERARY_ID_AB, USER_ID, BigDecimal.ZERO);

        // Assert 1: Settlement is PAYMENT_FAILED with a failure reason
        assertThat(failed.getStatus()).isEqualTo(Settlement.SettlementStatus.FAILED);
        assertThat(failed.getFailureReason()).isNotBlank();

        // Assert 2: payment.failed event published to RabbitMQ
        Awaitility.await("payment.failed published")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    Object received = rabbitTemplate.receiveAndConvert(failedQueue);
                    assertThat(received)
                            .as("Expected PaymentFailedEvent on payment.failed queue")
                            .isNotNull()
                            .isInstanceOf(PaymentFailedEvent.class);
                    PaymentFailedEvent pfe = (PaymentFailedEvent) received;
                    assertThat(pfe.itineraryId()).isEqualTo(ITINERARY_ID_AB);
                    assertThat(pfe.reason()).isNotBlank();
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario C — Pre-check failure (no CONFIRMED bookings)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("C) confirmed-summary count=0 when only PENDING bookings → saga aborts at S3")
    void scenarioC_noConfirmedBookings() {
        // Arrange: seed parent itineraries row (FK from bookings.itin_id → itineraries.id)
        jdbcTemplate.execute(
                "MERGE INTO itineraries (id, user_id, title, status) " +
                "KEY(id) VALUES (" + ITINERARY_ID_C + ", " + USER_ID + ", 'Test', 'IN_PROGRESS')");

        // Arrange: insert a PENDING booking via JDBC to bypass the @JdbcTypeCode(JSON)
        // H2 issue on bookingDetails — we only need the row to exist with status=PENDING
        jdbcTemplate.execute(
                "INSERT INTO bookings (itin_id, user_id, amount, type, status, booking_details, created_at) " +
                "VALUES (" + ITINERARY_ID_C + ", " + USER_ID + ", 500.0, 'ACCOMMODATION', 'PENDING', '{}', NOW())");

        // Act: itinerary-service calls this before deciding whether to publish the event
        var summary = bookingAggregateService.getConfirmedSummary(ITINERARY_ID_C);

        // Assert 1: count=0 — only PENDING, not CONFIRMED → itinerary-service aborts with 400
        assertThat(summary.count()).isZero();
        assertThat(summary.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);

        // Assert 2: no settlement row — event was never published/consumed
        assertThat(settlementRepository.findByItineraryId(ITINERARY_ID_C)).isEmpty();
    }
}
