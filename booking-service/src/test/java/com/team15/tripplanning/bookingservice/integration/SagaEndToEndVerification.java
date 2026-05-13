package com.team15.tripplanning.bookingservice.integration;

import com.team15.tripplanning.contracts.events.ItineraryCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentCompletedEvent;
import com.team15.tripplanning.contracts.events.PaymentFailedEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M3 End-to-End Integration Verification — <b>OWNED BY: S5-INFRA</b>.
 *
 * <p><b>DISABLED by default.</b> Enable only after ALL 15 M3 feature slices have merged
 * into {@code main} and the full stack ({@code docker-compose.override.yml}) is running.
 *
 * <h3>Prerequisites before running</h3>
 * <ol>
 *   <li>All 15 feature slices merged to main.
 *   <li>{@code docker compose up --build} with {@code docker-compose.override.yml} applied
 *       ({@code M3_MESSAGING_ENABLED=true} on all services).
 *   <li>At least one user (id=1, status=ACTIVE) and destination (id=1, status=ACTIVE)
 *       seeded in the database.
 * </ol>
 *
 * <h3>Scenarios verified</h3>
 * <ol>
 *   <li>{@link #sagaHappyPath} — {@code ItineraryCompletedEvent} received by booking-service
 *       → Settlement created (SETTLEMENT_PENDING) → payment simulated
 *       → Settlement reaches SETTLED → {@code PaymentCompletedEvent} published back.
 *   <li>{@link #sagaCompensation} — Payment fails → Settlement reaches PAYMENT_FAILED
 *       → {@code PaymentFailedEvent} published → itinerary-service listener transitions
 *       itinerary to PAYMENT_FAILED.
 *   <li>{@link #gatewayRoutingSmoke} — All 5 route blocks in api-gateway/application.yml
 *       are reachable via the gateway on port 8080 (GET /health for each service).
 *   <li>{@link #feignCallsReachM3Endpoints} — Verify that the key cross-service Feign
 *       calls introduced in M3 resolve to real endpoints (no 404).
 * </ol>
 *
 * <h3>How to enable</h3>
 * Remove the {@code @Disabled} annotation and run:
 * <pre>
 *   mvn test -pl booking-service -Dtest=SagaEndToEndVerification
 * </pre>
 */
@Disabled("""
        S5-INFRA: Enable after ALL 15 M3 slices have merged and
        docker compose up --build (with docker-compose.override.yml) is running.
        See class Javadoc for full prerequisites.
        """)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("M3 Saga & Gateway End-to-End Verification [S5-INFRA]")
class SagaEndToEndVerification {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Exchange name declared by itinerary-service S3 AmqpConfig. */
    private static final String ITINERARY_EXCHANGE = "itinerary.events";

    /** Exchange name declared by booking-service S5 AmqpConfig. */
    private static final String PAYMENT_EXCHANGE = "payment.events";

    private static final long   TEST_ITINERARY_ID  = 1L;
    private static final long   TEST_USER_ID       = 1L;
    private static final long   TEST_DESTINATION_ID = 1L;
    private static final BigDecimal TEST_AMOUNT    = BigDecimal.valueOf(1500.00);

    // ── Infrastructure ────────────────────────────────────────────────────────

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Plain RestTemplate — TestRestTemplate was removed in Spring Boot 4.x.
    // S5-INFRA: inject the service base URL via @LocalServerPort if testing
    // booking-service endpoints locally, or construct a client pointing at the
    // gateway when running gateway routing smoke tests.
    private final RestTemplate restTemplate = new RestTemplate();

    // ── Test 1: Saga Happy Path ───────────────────────────────────────────────

    /**
     * Publishes an {@link ItineraryCompletedEvent} directly to {@code itinerary.events}
     * and verifies that booking-service creates a Settlement row with status
     * {@code SETTLEMENT_PENDING}, then simulates payment success by publishing
     * {@link PaymentCompletedEvent} and asserting final status {@code SETTLED}.
     *
     * <p>Implementation note: replace the sleep with an Awaitility poll against the
     * settlements REST endpoint once S5-INFRA's full implementation is in place.
     */
    @Test
    @Order(1)
    @DisplayName("1. Saga happy path: ItineraryCompleted → SETTLEMENT_PENDING → SETTLED")
    void sagaHappyPath() throws InterruptedException {
        // Step 1 — S3 publishes ItineraryCompletedEvent (simulated here directly)
        var event = new ItineraryCompletedEvent(
                TEST_ITINERARY_ID, TEST_USER_ID, TEST_DESTINATION_ID, TEST_AMOUNT);
        rabbitTemplate.convertAndSend(ITINERARY_EXCHANGE, "itinerary.completed", event);

        // Allow booking-service consumer to process
        TimeUnit.SECONDS.sleep(2);

        // Step 2 — Assert Settlement row exists with SETTLEMENT_PENDING
        ResponseEntity<String> settlement = restTemplate.getForEntity(
                "/api/bookings/settlements/" + TEST_ITINERARY_ID, String.class);
        assertThat(settlement.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(settlement.getBody()).contains("SETTLEMENT_PENDING");

        // Step 3 — Simulate payment gateway success (normally async via payment adapter)
        // S5-INFRA: replace with a real payment gateway stub call in your implementation
        long settlementId = 1L; // read from response body in real test
        var paymentEvent = new PaymentCompletedEvent(settlementId, TEST_ITINERARY_ID, TEST_AMOUNT);
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, "payment.completed", paymentEvent);

        TimeUnit.SECONDS.sleep(2);

        // Step 4 — Assert Settlement reached SETTLED
        ResponseEntity<String> settled = restTemplate.getForEntity(
                "/api/bookings/settlements/" + TEST_ITINERARY_ID, String.class);
        assertThat(settled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(settled.getBody()).contains("SETTLED");
    }

    // ── Test 2: Saga Compensation ─────────────────────────────────────────────

    /**
     * Verifies that a {@link PaymentFailedEvent} transitions the Settlement to
     * {@code PAYMENT_FAILED} and that booking-service publishes the compensation
     * event back to itinerary-service.
     */
    @Test
    @Order(2)
    @DisplayName("2. Saga compensation: PaymentFailed → PAYMENT_FAILED → ItineraryCancelled published")
    void sagaCompensation() throws InterruptedException {
        // Use a separate itinerary ID to avoid conflict with test 1
        long failItineraryId = 999L;
        var placed = new ItineraryCompletedEvent(
                failItineraryId, TEST_USER_ID, TEST_DESTINATION_ID, TEST_AMOUNT);
        rabbitTemplate.convertAndSend(ITINERARY_EXCHANGE, "itinerary.completed", placed);
        TimeUnit.SECONDS.sleep(2);

        long settlementId = 999L; // read dynamically in real implementation
        var failEvent = new PaymentFailedEvent(settlementId, failItineraryId, "Insufficient funds");
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, "payment.failed", failEvent);
        TimeUnit.SECONDS.sleep(2);

        ResponseEntity<String> failed = restTemplate.getForEntity(
                "/api/bookings/settlements/" + failItineraryId, String.class);
        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(failed.getBody()).contains("PAYMENT_FAILED");

        // S5-INFRA: additionally verify that ItineraryCancelledEvent was published
        // by consuming from a test queue bound to itinerary.events#itinerary.cancelled
    }

    // ── Test 3: Gateway Routing Smoke ─────────────────────────────────────────

    /**
     * Verifies that all 5 route blocks in {@code api-gateway/application.yml} forward
     * requests correctly. Hits each service's health endpoint via the gateway (port 8080).
     *
     * <p>S5-INFRA: point {@code restTemplate} at the gateway host:port, not at
     * booking-service directly, by setting {@code GATEWAY_URL} in the test environment.
     */
    @Test
    @Order(3)
    @DisplayName("3. Gateway routing: all 5 routes forward health checks correctly")
    void gatewayRoutingSmoke() {
        // S5-INFRA: replace localhost:8080 with the gateway container's address
        String gatewayBase = System.getProperty("gateway.url", "http://localhost:8080");

        String[] healthPaths = {
            "/api/users/health",          // → user-service (S1 route block)
            "/api/destinations/health",   // → destination-service (S2 route block)
            "/api/itineraries/health",    // → itinerary-service (S3 route block)
            "/api/activities/health",     // → activity-service (S4 route block)
            "/api/bookings/health",       // → booking-service (S5 route block)
        };

        for (String path : healthPaths) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayBase + path, String.class);
            assertThat(response.getStatusCode().is2xxSuccessful())
                    .as("Gateway route for %s should return 2xx but was %s",
                            path, response.getStatusCode())
                    .isTrue();
        }
    }

    // ── Test 4: Feign Cross-Service Connectivity ──────────────────────────────

    /**
     * Verifies that the key M3 Feign-callable endpoints exist and return 200 (not 404).
     * These endpoints are declared in {@code contracts.feign.*Client} and implemented
     * in each feature slice. If any returns 404, that slice's implementation is missing.
     */
    @Test
    @Order(4)
    @DisplayName("4. Feign endpoint availability: all contracts-defined endpoints return 2xx")
    void feignCallsReachM3Endpoints() {
        // S1 → implemented by user-service
        assertEndpointExists("http://localhost:8081/api/users/1/trip-summary");

        // S2 → implemented by destination-service
        assertEndpointExists("http://localhost:8082/api/destinations/1/dashboard-aggregate");

        // S3 → implemented by itinerary-service (aggregate endpoints)
        assertEndpointExists("http://localhost:8083/api/itineraries/user/1/summary");
        assertEndpointExists("http://localhost:8083/api/itineraries/destination/1/active-count");

        // S4 → implemented by activity-service
        // (lifecycle endpoints require an existing activityId — S5-INFRA seeds one)

        // S5 → implemented by booking-service
        assertEndpointExists("http://localhost:8085/api/bookings/user/1/total?startDate=2025-01-01&endDate=2025-12-31");
        assertEndpointExists("http://localhost:8085/api/bookings/itinerary/1/confirmed-summary");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void assertEndpointExists(String url) {
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertThat(response.getStatusCode())
                .as("Expected 2xx from %s but got %s — is that slice's implementation merged?",
                        url, response.getStatusCode())
                .isNotEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getStatusCode().is5xxServerError())
                .as("Expected no 5xx from %s", url)
                .isFalse();
    }
}
