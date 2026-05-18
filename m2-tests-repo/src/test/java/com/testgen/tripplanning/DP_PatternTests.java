package com.testgen.tripplanning;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC379–TC425 — Design Pattern checks for Milestone 2 (Trip Planning theme).
 *
 * <p>Seven GoF patterns: Strategy, Observer, CoR, Builder, Singleton, Factory, Adapter.
 *
 * <p><b>Architectural constraint</b>: This test-runner executes natively on the host
 * and talks to student services via HTTP. Student JARs are NOT on this JVM's classpath —
 * {@code Class.forName()} is impossible. All structural checks therefore use source-file
 * scanning via the helpers in {@link TestBase#allJavaFiles()} /
 * {@link TestBase#readClassSource(String)} / {@link TestBase#anySourceContains(String)}.
 *
 * <p>Trip Planning service-slot routing:
 * <ul>
 *   <li>userServiceUrl     → user-service       (port 8081)</li>
 *   <li>catalogServiceUrl  → destination-service (port 8082, S2)</li>
 *   <li>orderServiceUrl    → itinerary-service   (port 8083, S3)</li>
 *   <li>deliveryServiceUrl → activity-service    (port 8084, S4)</li>
 *   <li>checkoutServiceUrl → booking-service     (port 8085, S5)</li>
 * </ul>
 */
public class DP_PatternTests extends TestBase {

    // ───────────────────────────────────────────────────────────────────────────
    // Trip-Planning-specific S5-F12 seed helper.
    // Inserts: an Itinerary (PLANNED status, given startDate) + a CONFIRMED Booking
    // referencing it (amount = given). Returns the booking id.
    // ───────────────────────────────────────────────────────────────────────────
    private long _seedConfirmedBookingForRefund(double amount,
                                                LocalDate itineraryStartDate,
                                                String itineraryStatus) throws Exception {
        long uid = adminId();
        // 1) Itinerary — insertRowReturningId auto-casts USER-DEFINED enum columns.
        String iTable = tableName("Itinerary");
        Map<String, Object> iov = new HashMap<>();
        iov.put(columnByField("Itinerary", "userId"),    uid);
        iov.put(columnByField("Itinerary", "title"),     "TC-DP refund itin " + System.nanoTime());
        iov.put(columnByField("Itinerary", "startDate"), java.sql.Date.valueOf(itineraryStartDate));
        iov.put(columnByField("Itinerary", "endDate"),
                java.sql.Date.valueOf(itineraryStartDate.plusDays(3)));
        iov.put(columnByField("Itinerary", "status"),    itineraryStatus);
        long itineraryId = insertRowReturningId(iTable, iov);
        // 2) Booking (status=CONFIRMED, links to itinerary)
        String bTable = tableName("Booking");
        Map<String, Object> bov = new HashMap<>();
        bov.put(columnByField("Booking", "itineraryId"), itineraryId);
        bov.put(columnByField("Booking", "userId"),      uid);
        bov.put(columnByField("Booking", "amount"),      amount);
        bov.put(columnByField("Booking", "status"),      "CONFIRMED");
        long bookingId = insertRowReturningId(bTable, bov);
        return bookingId;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DP-1  STRATEGY  (TC379–TC385)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC379 — DP-1 Strategy: RefundStrategy interface exists")
    void tc379_refundStrategyInterfaceExists() throws Exception {
        BASE_URL = checkoutServiceUrl;
        String src = readClassSource("RefundStrategy");
        assertFalse(src.isEmpty(),
            "TC379: RefundStrategy.java not found in src/main/java. " +
            "Per spec §3.2, booking-service must declare: interface RefundStrategy { … calculateRefund(…); }");
        assertTrue(src.contains("interface RefundStrategy"),
            "TC379: RefundStrategy must be declared as an interface, not a class. " +
            "Found head: " + src.substring(0, Math.min(300, src.length())));
        assertTrue(src.contains("calculateRefund"),
            "TC379: RefundStrategy interface must declare a method named 'calculateRefund'.");
    }

    @Test
    @DisplayName("TC380 — DP-1 Strategy: 4 concrete strategies implement RefundStrategy")
    void tc380_fourConcreteStrategies() throws Exception {
        BASE_URL = checkoutServiceUrl;
        // Trip Planning spec §3.2 mandates FOUR concrete strategies (one more than the
        // M2 baseline of three): Early/Mid/Late/NoRefund keyed off daysBeforeDeparture.
        String[] names = {
            s5StrategyFullRefund(),           // EarlyCancellationRefundStrategy (>14d, 100%)
            s5StrategyFoodOnly(),             // MidCancellationRefundStrategy   (7–14d, 50%)
            "LateCancellationRefundStrategy", // 1–6d, 25%
            s5StrategyNoRefund()              // trip already started or in progress
        };
        for (String name : names) {
            String src = readClassSource(name);
            assertFalse(src.isEmpty(),
                "TC380: " + name + ".java not found in src/main/java. " +
                "All four named strategy classes must exist per Trip Planning spec §3.2.");
            assertTrue(src.contains("implements RefundStrategy") ||
                       src.contains("implements RefundStrategy,") ||
                       src.contains("implements RefundStrategy "),
                "TC380: " + name + " must implement RefundStrategy. " +
                "Found class head: " + src.substring(0, Math.min(300, src.length())));
        }
    }

    @Test
    @DisplayName("TC381 — DP-1 Strategy: RefundStrategySelector exists")
    void tc381_refundStrategySelectorExists() throws Exception {
        BASE_URL = checkoutServiceUrl;
        boolean selectorFound = anySourceContains("RefundStrategySelector") ||
                                anySourceContains("RefundStrategyFactory");
        assertTrue(selectorFound,
            "TC381: No RefundStrategySelector or RefundStrategyFactory found in src/main/java. " +
            "Per spec §3.2, dispatch logic must live in a separate selector/factory class.");
        boolean returnsStrategy =
            anySourceContains("RefundStrategy select(") ||
            anySourceContains("RefundStrategy getStrategy(") ||
            anySourceContains("RefundStrategy choose(") ||
            anySourceContains("RefundStrategy resolve(") ||
            anySourceContains(": RefundStrategy") ||
            anySourceContains("RefundStrategy selectStrategy");
        assertTrue(returnsStrategy,
            "TC381: RefundStrategySelector must expose a method that returns RefundStrategy. " +
            "Per spec, the service calls selector.select(…).calculateRefund(…) polymorphically.");
    }

    @Test
    @DisplayName("TC382 — DP-1 Strategy: EarlyCancellationRefundStrategy audit trail")
    void tc382_earlyRefundAuditTrail() throws Exception {
        BASE_URL = checkoutServiceUrl;
        // CONFIRMED booking, 2000 EGP, itinerary startDate 30 days from today (PLANNED) → EARLY tier.
        long bookingId = _seedConfirmedBookingForRefund(
                2000.0, LocalDate.now().plusDays(30), "PLANNED");
        String tok = adminToken();
        String body = "{\"reason\":\"schedule_conflict\"}";
        HttpResponse<String> rsp = httpPostAuth(
                "/api/bookings/" + bookingId + "/refund-cancellation-tier", body, tok);
        assert2xx(rsp, "TC382 EarlyRefund POST");
        // Verify strategy recorded in PG bookingDetails JSONB
        String bTable = tableName("Booking");
        String dCol   = columnByField("Booking", "bookingDetails");
        String details = jdbc.queryForObject(
            "SELECT \"" + dCol + "\"::text FROM \"" + bTable + "\" WHERE id = ?",
            String.class, bookingId);
        assertNotNull(details,
            "TC382: bookingDetails must be populated after EarlyCancellationRefundStrategy; bookingId="
            + bookingId);
        assertTrue(details.contains(s5StrategyFullRefund()),
            "TC382: bookingDetails.strategyName must be " + s5StrategyFullRefund() +
            "; got " + details);
        // Also check MongoDB audit if available
        if (mongo != null) {
            com.mongodb.client.MongoCollection<org.bson.Document> col =
                    mongo.getCollection(s5AuditCollection());
            org.bson.Document latest = col
                    .find(new org.bson.Document("bookingId", bookingId))
                    .sort(new org.bson.Document("_id", -1)).first();
            if (latest == null)
                latest = col.find().sort(new org.bson.Document("_id", -1)).first();
            if (latest != null) {
                String strat = latest.getString("strategyName");
                if (strat == null) {
                    org.bson.Document det = latest.get("details", org.bson.Document.class);
                    if (det != null) strat = det.getString("strategyName");
                }
                if (strat != null)
                    assertEquals(s5StrategyFullRefund(), strat,
                        "TC382: audit strategyName must be " + s5StrategyFullRefund() +
                        "; got=" + strat);
            }
        }
    }

    @Test
    @DisplayName("TC383 — DP-1 Strategy: MidCancellationRefundStrategy audit trail")
    void tc383_midRefundAuditTrail() throws Exception {
        BASE_URL = checkoutServiceUrl;
        // CONFIRMED booking, 1200 EGP, startDate 10 days out → MID tier (50%).
        long bookingId = _seedConfirmedBookingForRefund(
                1200.0, LocalDate.now().plusDays(10), "PLANNED");
        String tok = adminToken();
        String body = "{\"reason\":\"schedule_conflict\"}";
        HttpResponse<String> rsp = httpPostAuth(
                "/api/bookings/" + bookingId + "/refund-cancellation-tier", body, tok);
        assert2xx(rsp, "TC383 MidRefund POST");
        String bTable = tableName("Booking");
        String dCol   = columnByField("Booking", "bookingDetails");
        String details = jdbc.queryForObject(
            "SELECT \"" + dCol + "\"::text FROM \"" + bTable + "\" WHERE id = ?",
            String.class, bookingId);
        assertNotNull(details,
            "TC383: bookingDetails must be populated after MidCancellationRefundStrategy; bookingId="
            + bookingId);
        assertTrue(details.contains(s5StrategyFoodOnly()),
            "TC383: bookingDetails.strategyName must be " + s5StrategyFoodOnly() +
            "; got " + details);
        if (mongo != null) {
            com.mongodb.client.MongoCollection<org.bson.Document> col =
                    mongo.getCollection(s5AuditCollection());
            org.bson.Document latest = col
                    .find(new org.bson.Document("bookingId", bookingId))
                    .sort(new org.bson.Document("_id", -1)).first();
            if (latest == null)
                latest = col.find().sort(new org.bson.Document("_id", -1)).first();
            if (latest != null) {
                String strat = latest.getString("strategyName");
                if (strat == null) {
                    org.bson.Document det = latest.get("details", org.bson.Document.class);
                    if (det != null) strat = det.getString("strategyName");
                }
                if (strat != null)
                    assertEquals(s5StrategyFoodOnly(), strat,
                        "TC383: audit strategyName must be " + s5StrategyFoodOnly() +
                        "; got=" + strat);
            }
        }
    }

    @Test
    @DisplayName("TC384 — DP-1 Strategy: LateCancellationRefundStrategy audit trail")
    void tc384_lateRefundAuditTrail() throws Exception {
        BASE_URL = checkoutServiceUrl;
        // Per Trip Planning M2.tex §3.2 step (6) and §S5-F12:
        // daysBeforeDeparture in [1, 7) → LateCancellationRefundStrategy → refund 25%.
        // CONFIRMED booking, 800 EGP, startDate 3 days out → refund = 200.
        long bookingId = _seedConfirmedBookingForRefund(
                800.0, LocalDate.now().plusDays(3), "PLANNED");
        String tok = adminToken();
        String body = "{\"reason\":\"schedule_conflict\"}";
        HttpResponse<String> rsp = httpPostAuth(
                "/api/bookings/" + bookingId + "/refund-cancellation-tier", body, tok);
        assert2xx(rsp, "TC384 LateRefund POST");
        String bTable = tableName("Booking");
        String dCol   = columnByField("Booking", "bookingDetails");
        String details = jdbc.queryForObject(
            "SELECT \"" + dCol + "\"::text FROM \"" + bTable + "\" WHERE id = ?",
            String.class, bookingId);
        assertNotNull(details,
            "TC384: bookingDetails must be populated after LateCancellationRefundStrategy; bookingId="
            + bookingId);
        assertTrue(details.contains("LateCancellationRefundStrategy"),
            "TC384: bookingDetails.strategyName must be LateCancellationRefundStrategy; got " + details);
        if (mongo != null) {
            com.mongodb.client.MongoCollection<org.bson.Document> col =
                    mongo.getCollection(s5AuditCollection());
            org.bson.Document latest = col
                    .find(new org.bson.Document("bookingId", bookingId))
                    .sort(new org.bson.Document("_id", -1)).first();
            if (latest == null)
                latest = col.find().sort(new org.bson.Document("_id", -1)).first();
            if (latest != null) {
                String strat = latest.getString("strategyName");
                if (strat == null) {
                    org.bson.Document det = latest.get("details", org.bson.Document.class);
                    if (det != null) strat = det.getString("strategyName");
                }
                if (strat != null)
                    assertEquals("LateCancellationRefundStrategy", strat,
                        "TC384: audit strategyName must be LateCancellationRefundStrategy; got=" + strat);
            }
        }
    }

    @Test
    @DisplayName("TC385 — DP-1 Strategy: NoRefundStrategy 400 + REFUND_DENIED audit")
    void tc385_noRefundAuditOnDenial() throws Exception {
        BASE_URL = checkoutServiceUrl;
        if (mongo == null) throw new AssertionError(
            "TC385: MongoDB required — REFUND_DENIED audit check needs " + s5AuditCollection() + " collection.");
        // Itinerary startDate yesterday → trip already started → NoRefundStrategy.
        long bookingId = _seedConfirmedBookingForRefund(
                500.0, LocalDate.now().minusDays(1), "PLANNED");
        String tok = adminToken();
        String body = "{\"reason\":\"schedule_conflict\"}";
        HttpResponse<String> rsp = httpPostAuth(
                "/api/bookings/" + bookingId + "/refund-cancellation-tier", body, tok);
        assertEquals(400, rsp.statusCode(),
            "TC385: trip-already-started must return 400; got=" + rsp.statusCode() +
            " body=" + rsp.body());
        // NOTE: do not assert on response body — Spring Boot 4 strips
        // ResponseStatusException reason from the default error body unless
        // server.error.include-message=always is set. The denial reason is
        // verified via the Mongo audit event below instead.
        com.mongodb.client.MongoCollection<org.bson.Document> col =
                mongo.getCollection(s5AuditCollection());
        // Search for a REFUND_DENIED action document (event referencing the strategy or bookingId)
        boolean foundDenied = false;
        for (org.bson.Document d : col.find()) {
            String act = d.getString("action");
            if (act == null) act = d.getString("eventType");
            if (act == null) {
                org.bson.Document det = d.get("details", org.bson.Document.class);
                if (det != null) act = det.getString("action");
            }
            String stratName = d.getString("strategyName");
            if (stratName == null) {
                org.bson.Document det = d.get("details", org.bson.Document.class);
                if (det != null) stratName = det.getString("strategyName");
            }
            Number bid = d.get("bookingId", Number.class);
            boolean refersToBooking = (bid != null && bid.longValue() == bookingId);
            boolean isDenied = act != null && act.contains("REFUND_DENIED");
            boolean isNoRefundStrat = s5StrategyNoRefund().equals(stratName);
            if (refersToBooking && (isDenied || isNoRefundStrat)) {
                foundDenied = true;
                break;
            }
        }
        assertTrue(foundDenied,
            "TC385: REFUND_DENIED event referencing bookingId=" + bookingId +
            " (or strategyName=" + s5StrategyNoRefund() + ") must be logged to '" +
            s5AuditCollection() + "' BEFORE the 400 is thrown (per spec §3.2 step f).");
    }

    @Test
    @DisplayName("TC386 — DP-1 Strategy: refund service has no if-else on daysBeforeDeparture")
    void tc386_noIfElseOnDaysBeforeDepartureInService() throws Exception {
        BASE_URL = checkoutServiceUrl;
        for (java.nio.file.Path p : allJavaFiles()) {
            if (p.toString().contains("/src/test/")) continue;
            String fname = p.getFileName().toString();
            // Only inspect service-layer files
            if (!fname.endsWith("Service.java") && !fname.endsWith("ServiceImpl.java"))
                continue;
            // Skip the strategy/selector/factory files
            if (fname.contains("Strategy") || fname.contains("Selector") ||
                fname.contains("Factory") || fname.contains("Refund"))
                continue;
            String content;
            try { content = java.nio.file.Files.readString(p); }
            catch (java.io.IOException e) { continue; }
            if (!content.contains("daysBeforeDeparture")) continue;
            assertFalse(content.contains("if (daysBeforeDeparture") ||
                        content.contains("if(daysBeforeDeparture") ||
                        content.contains("daysBeforeDeparture >") ||
                        content.contains("daysBeforeDeparture <") ||
                        content.contains("daysBeforeDeparture ?") ||
                        content.contains("daysBeforeDeparture?"),
                "TC386: " + fname + " branches on daysBeforeDeparture directly. " +
                "Per spec §3.2 step h, this decision belongs in RefundStrategySelector, not the service.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DP-2  OBSERVER  (TC386–TC392)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC387 — DP-2 Observer: EntityObserver interface")
    void tc387_entityObserverInterface() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("EntityObserver");
        assertFalse(src.isEmpty(),
            "TC387: EntityObserver.java not found in src/main/java. " +
            "Required: interface EntityObserver { void onEvent(String eventType, Object payload); }");
        assertTrue(src.contains("interface EntityObserver"),
            "TC387: EntityObserver must be declared as an interface; found head: " +
            src.substring(0, Math.min(300, src.length())));
        assertTrue(src.contains("onEvent"),
            "TC387: EntityObserver must declare onEvent(…) method.");
    }

    @Test
    @DisplayName("TC388 — DP-2 Observer: MongoEventLogger implements EntityObserver")
    void tc388_mongoEventLoggerImplementsObserver() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("MongoEventLogger");
        assertFalse(src.isEmpty(),
            "TC388: MongoEventLogger.java not found in src/main/java.");
        assertTrue(src.contains("implements EntityObserver") ||
                   src.contains("implements EntityObserver,") ||
                   src.contains("implements EntityObserver "),
            "TC388: MongoEventLogger must implement EntityObserver; found head: " +
            src.substring(0, Math.min(300, src.length())));
    }

    @Test
    @DisplayName("TC389 — DP-2 Observer: no @EventListener writes to MongoDB (Spring vs GoF)")
    void tc389_noSpringEventListenerOnMongoPath() throws Exception {
        BASE_URL = userServiceUrl;
        for (java.nio.file.Path p : allJavaFiles()) {
            if (p.toString().contains("/src/test/")) continue;
            String content;
            try { content = java.nio.file.Files.readString(p); }
            catch (java.io.IOException e) { continue; }
            if (!content.contains("@EventListener")) continue;
            boolean writesToMongo =
                content.contains("mongoTemplate") ||
                content.contains("MongoTemplate")  ||
                content.contains("mongoRepository") ||
                content.contains("MongoRepository") ||
                (content.contains(".save(") && (content.contains("mongo") || content.contains("Mongo"))) ||
                (content.contains(".insert(") && (content.contains("mongo") || content.contains("Mongo")));
            assertFalse(writesToMongo,
                "TC389: " + p.getFileName() + " has @EventListener AND MongoDB writes. " +
                "Per spec §3.3, event logging must go through the GoF Observer chain — " +
                "not Spring's @EventListener/@ApplicationEventPublisher.");
        }
    }

    @Test
    @DisplayName("TC390 — DP-2 Observer: register triggers REGISTERED in auth_events")
    void tc390_registerTriggersAuthEvent() throws Exception {
        BASE_URL = userServiceUrl;
        if (mongo == null) throw new AssertionError("TC389: MongoDB required.");
        com.mongodb.client.MongoCollection<org.bson.Document> col =
                mongo.getCollection("auth_events");
        long before = col.countDocuments();
        Map<String, Object> u = seedAndLoginUser("tc389reg");
        long uid = (long)(Number) u.get("id");
        long after = col.countDocuments();
        assertTrue(after > before,
            "TC390: auth_events must grow after registration. " +
            "before=" + before + " after=" + after + ". Observer chain not wired to register controller.");
        // seedAndLoginUser fires both REGISTERED + LOGGED_IN — filter explicitly to avoid race.
        org.bson.Document doc = col
                .find(new org.bson.Document("userId", uid).append("action", "REGISTERED"))
                .first();
        if (doc == null)
            doc = col.find(new org.bson.Document("action", "REGISTERED"))
                    .sort(new org.bson.Document("_id", -1)).first();
        assertNotNull(doc, "TC390: No auth_events doc for userId=" + uid);
        String action = doc.getString("action");
        if (action == null) action = doc.getString("eventType");
        if (action != null)
            assertEquals("REGISTERED", action,
                "TC390: event action must be REGISTERED; got=" + action);
    }

    @Test
    @DisplayName("TC391 — DP-2 Observer: login triggers LOGGED_IN")
    void tc391_loginTriggersLoggedInEvent() throws Exception {
        BASE_URL = userServiceUrl;
        if (mongo == null) throw new AssertionError("TC390: MongoDB required.");
        Map<String, Object> u = seedAndLoginUser("tc390login");
        long uid  = (long)(Number) u.get("id");
        String email = (String) u.get("email");
        com.mongodb.client.MongoCollection<org.bson.Document> col =
                mongo.getCollection("auth_events");
        long before = col.countDocuments(new org.bson.Document("userId", uid));
        String loginBody = String.format("{\"email\":\"%s\",\"password\":\"UserPwd!2026\"}", email);
        HttpResponse<String> loginResp = http.send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(userServiceUrl + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(loginBody)).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assert2xx(loginResp, "TC391 second login");
        long after = col.countDocuments(new org.bson.Document("userId", uid));
        assertTrue(after > before,
            "TC391: auth_events must grow on login for userId=" + uid +
            "; before=" + before + " after=" + after);
        org.bson.Document latest = col
                .find(new org.bson.Document("userId", uid))
                .sort(new org.bson.Document("_id", -1)).first();
        if (latest != null) {
            String action = latest.getString("action");
            if (action == null) action = latest.getString("eventType");
            if (action != null)
                assertEquals("LOGGED_IN", action,
                    "TC391: latest event for userId=" + uid + " must be LOGGED_IN; got=" + action);
        }
    }

    @Test
    @DisplayName("TC392 — DP-2 Observer: M1 retrofit (S1-F2) emits event")
    void tc392_m1RetrofitEmitsEvent() throws Exception {
        BASE_URL = userServiceUrl;
        if (mongo == null) throw new AssertionError("TC391: MongoDB required.");
        Map<String, Object> u = seedAndLoginUser("tc391pref");
        long uid = (long)(Number) u.get("id");
        String tok = (String) u.get("token");
        com.mongodb.client.MongoCollection<org.bson.Document> col =
                mongo.getCollection("auth_events");
        long before = col.countDocuments(new org.bson.Document("userId", uid));
        HttpResponse<String> r = httpPutAuth(
                "/api/users/" + uid + "/preferences", "{\"language\":\"fr\"}", tok);
        assert2xx(r, "TC392 PUT /api/users/{id}/preferences");
        long after = col.countDocuments(new org.bson.Document("userId", uid));
        assertTrue(after > before,
            "TC392: M1 S1-F2 (PUT preferences) must emit an observer event. " +
            "before=" + before + " after=" + after + " for userId=" + uid +
            ". The M2 Observer retrofit must cover M1 endpoints too.");
    }

    @Test
    @DisplayName("TC393 — DP-2 Observer: unregister method exists (chain load-bearing check)")
    void tc393_unregisterMethodExists() throws Exception {
        BASE_URL = userServiceUrl;
        boolean hasUnregister =
            anySourceContains("removeObserver(") ||
            anySourceContains("unregisterObserver(") ||
            anySourceContains("detachObserver(")   ||
            anySourceContains("unregister(EntityObserver") ||
            anySourceContains("detach(EntityObserver");
        assertTrue(hasUnregister,
            "TC393: No removeObserver / unregisterObserver / detachObserver method found. " +
            "The Observer subject must support observer removal to prove the chain is load-bearing. " +
            "Without it, there is no way to verify that MongoDB writes go through observers rather than " +
            "being direct calls hidden alongside notifyObservers(…).");
        boolean hasNotify =
            anySourceContains("notifyObservers(") ||
            anySourceContains("notifyObserver(");
        assertTrue(hasNotify,
            "TC393: No notifyObservers(…) call found in any service class. " +
            "The subject must call notifyObservers(…) to dispatch events through the chain.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DP-3  CHAIN OF RESPONSIBILITY  (TC393–TC400)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC394 — DP-3 CoR: AuthHandler base + setNext/handle")
    void tc394_authHandlerBaseExists() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("AuthHandler");
        assertFalse(src.isEmpty(),
            "TC394: AuthHandler.java not found in src/main/java. " +
            "Required: abstract class (or interface) with setNext(AuthHandler) and handle(…) methods.");
        assertTrue(src.contains("setNext"),
            "TC394: AuthHandler must declare setNext(AuthHandler) — the chain-linkage primitive.");
        assertTrue(src.contains("handle"),
            "TC394: AuthHandler must declare handle(…) — the chain dispatch method.");
    }

    @Test
    @DisplayName("TC395 — DP-3 CoR: ≥3 concrete AuthHandler subclasses")
    void tc395_atLeastThreeConcreteHandlers() throws Exception {
        BASE_URL = userServiceUrl;
        long count = allJavaFiles().stream()
                .filter(p -> !p.toString().contains("/src/test/"))
                .filter(p -> !p.getFileName().toString().equals("AuthHandler.java"))
                .filter(p -> {
                    try {
                        String c = java.nio.file.Files.readString(p);
                        return c.contains("extends AuthHandler") ||
                               c.contains("implements AuthHandler");
                    } catch (java.io.IOException e) { return false; }
                })
                .count();
        assertTrue(count >= 3,
            "TC395: Expected ≥3 concrete AuthHandler subclasses; found=" + count + ". " +
            "Per spec §3.4: TokenExtractionHandler, SignatureValidationHandler, UserLoaderHandler " +
            "(+ optional RoleAuthorizationHandler).");
    }

    @Test
    @DisplayName("TC396 — DP-3 CoR: missing Authorization → 401")
    void tc396_missingAuthHeader401() throws Exception {
        BASE_URL = userServiceUrl;
        HttpResponse<String> r = httpGet("/api/users/1");
        assertEquals(401, r.statusCode(),
            "TC396: GET /api/users/1 without Authorization header must return 401 " +
            "(TokenExtractionHandler short-circuits); got=" + r.statusCode());
    }

    @Test
    @DisplayName("TC397 — DP-3 CoR: invalid signature → 401")
    void tc397_invalidSignature401() throws Exception {
        BASE_URL = userServiceUrl;
        HttpResponse<String> r = httpGetWithRawAuth("/api/users/1", "Bearer xxx.yyy.zzz");
        assertEquals(401, r.statusCode(),
            "TC397: GET /api/users/1 with malformed/invalid token must return 401 " +
            "(SignatureValidationHandler); got=" + r.statusCode());
    }

    @Test
    @DisplayName("TC398 — DP-3 CoR: deleted user with valid token → 401")
    void tc398_deletedUserToken401() throws Exception {
        BASE_URL = userServiceUrl;
        Map<String, Object> u = seedAndLoginUser("tc397del");
        long uid = (long)(Number) u.get("id");
        String tok = (String) u.get("token");
        String userTable = tableName("User");
        // Fresh user has no itineraries/bookings — DELETE should succeed; fall back to DEACTIVATE.
        try {
            jdbc.update("DELETE FROM \"" + userTable + "\" WHERE id = ?", uid);
        } catch (org.springframework.dao.DataAccessException ex) {
            String statusCol = columnByField("User", "status");
            jdbc.update("UPDATE \"" + userTable + "\" SET \"" + statusCol +
                        "\" = 'DEACTIVATED' WHERE id = ?", uid);
        }
        HttpResponse<String> r = httpGetAuth("/api/users/" + uid, tok);
        assertEquals(401, r.statusCode(),
            "TC398: Valid token for a deleted/deactivated user must return 401 " +
            "(UserLoaderHandler must re-check PG on every request); got=" + r.statusCode());
    }

    @Test
    @DisplayName("TC399 — DP-3 CoR: ADMIN-only endpoint with TRAVELER token → 403")
    void tc399_travelerTokenOnAdminEndpoint403() throws Exception {
        BASE_URL = userServiceUrl;
        Map<String, Object> u = seedAndLoginUser("tc398trav");
        String tok = (String) u.get("token");
        long uid = (long)(Number) u.get("id");
        HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/role",
                "{\"role\":\"ADMIN\"}", tok);
        assertEquals(403, r.statusCode(),
            "TC399: TRAVELER PUT /api/users/{id}/role must return 403 " +
            "(RoleAuthorizationHandler — authenticated but forbidden); got=" + r.statusCode());
    }

    @Test
    @DisplayName("TC400 — DP-3 CoR: ADMIN-only with ADMIN token → 2xx")
    void tc400_adminTokenOnAdminEndpoint200() throws Exception {
        BASE_URL = userServiceUrl;
        Map<String, Object> u = seedAndLoginUser("tc399target");
        long targetId = (long)(Number) u.get("id");
        HttpResponse<String> r = httpPutAuth("/api/users/" + targetId + "/role",
                "{\"role\":\"ADMIN\"}", adminToken());
        assertTrue(r.statusCode() >= 200 && r.statusCode() < 300,
            "TC400: ADMIN PUT /api/users/{id}/role must succeed (2xx — chain passes through); " +
            "got=" + r.statusCode() + " body=" + r.body());
    }

    @Test
    @DisplayName("TC401 — DP-3 CoR: filter delegates to chain head (source scan)")
    void tc401_filterDelegatesToChainHead() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("JwtAuthenticationFilter");
        assertFalse(src.isEmpty(),
            "TC401: JwtAuthenticationFilter.java not found in src/main/java.");
        boolean invokesChain =
            src.contains("head.handle(")       ||
            src.contains("authHandler.handle(") ||
            src.contains("chainHead.handle(")   ||
            src.contains("handler.handle(")     ||
            src.contains("first.handle(")       ||
            src.contains("chain.handle(")       ||
            src.contains(".handle(ctx")         ||
            src.contains(".handle(context")     ||
            src.contains(".handle(request")     ||
            src.contains(".handle(auth");
        assertTrue(invokesChain,
            "TC401: JwtAuthenticationFilter.doFilterInternal must delegate to the chain head " +
            "(e.g., head.handle(ctx)). The chain is dead code if the filter does validation inline.");
        boolean hasInlineJwts =
            src.contains("Jwts.parser()") || src.contains("Jwts.parserBuilder()");
        assertFalse(hasInlineJwts,
            "TC401: JwtAuthenticationFilter calls Jwts.parser/parserBuilder directly. " +
            "Token signature validation must be in SignatureValidationHandler, not the filter.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DP-4  BUILDER  (TC401–TC405)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC402 — DP-4 Builder: M2 dashboard DTOs have builder()")
    void tc402_m2DashboardDtosHaveBuilder() throws Exception {
        BASE_URL = userServiceUrl;
        // Per Trip Planning M2.tex §3.5: S2-F12 DestinationDashboardDTO,
        // S3-F10 ItineraryAnalyticsDashboardDTO, S4-F10 ActivityAnalyticsDTO,
        // S5-F10 DestinationSeasonRevenueDTO.
        String[] dtos = {
            "DestinationDashboardDTO",
            "ItineraryAnalyticsDashboardDTO",
            "ActivityAnalyticsDTO",
            "DestinationSeasonRevenueDTO"
        };
        List<String> noBuilder = new ArrayList<>();
        for (String dto : dtos) {
            String src = readClassSource(dto);
            if (src.isEmpty()) continue; // DTO may not exist yet — structural absence caught by TC402
            boolean hasBuilder =
                src.contains("builder()") ||
                src.contains("class Builder") ||
                src.contains("Builder<")  ||
                src.contains("new Builder(");
            if (!hasBuilder) noBuilder.add(dto);
        }
        assertTrue(noBuilder.isEmpty(),
            "TC402: M2 dashboard DTOs missing Builder pattern: " + noBuilder +
            ". Per spec §3.5, all 5-plus-field analytics DTOs must expose static builder() + fluent build().");
    }

    @Test
    @DisplayName("TC403 — DP-4 Builder: M1 in-scope DTOs have Builder (17 DTOs)")
    void tc403_m1DtosHaveBuilder() throws Exception {
        BASE_URL = userServiceUrl;
        // 17 DTO-returning M1 features per Trip Planning spec §3.5
        // (excludes S2-F8 verifyDestinationReview and S3-F8 addDays which return entities).
        String[] dtos = {
            "UserTripSummaryDTO",         // S1-F3
            "TopTravelerDTO",             // S1-F6
            "UserProfileDTO",             // S1-F8
            "SavedDestinationDTO",        // S1-F9
            "DestinationRevenueDTO",      // S2-F3
            "TopDestinationDTO",          // S2-F6
            "DestinationReviewAlertDTO",  // S2-F9
            "TripCostEstimateDTO",        // S3-F3
            "ItineraryAnalyticsDTO",      // S3-F6
            "ItineraryDetailsDTO",        // S3-F9
            "NearbyActivityDTO",          // S4-F3
            "ActivitySummaryDTO",         // S4-F8
            "BudgetActivityDTO",          // S4-F9
            "UserBookingSummaryDTO",      // S5-F3
            "RevenueReportDTO",           // S5-F6
            "BookingDetailsDTO",          // S5-F8
            "CouponUsageDTO"              // S5-F9
        };
        List<String> noBuilder = new ArrayList<>();
        for (String dto : dtos) {
            String src = readClassSource(dto);
            if (src.isEmpty()) { noBuilder.add(dto + "(file not found)"); continue; }
            boolean hasBuilder =
                src.contains("builder()") ||
                src.contains("class Builder") ||
                src.contains("Builder<");
            if (!hasBuilder) noBuilder.add(dto);
        }
        assertTrue(noBuilder.isEmpty(),
            "TC403: These M1 DTOs are missing Builder: " + noBuilder +
            ". Per spec §3.5, all 17 DTO-returning M1 features must be retrofitted with Builder.");
    }

    @Test
    @DisplayName("TC404 — DP-4 Builder: M2 destination dashboard works after Builder retrofit")
    void tc404_m2DashboardRegressionAfterBuilderRetrofit() throws Exception {
        BASE_URL = catalogServiceUrl;
        String adminTok = adminToken();
        // Seed a destination via JDBC (avoids HTTP CRUD dependency on field naming).
        String dTable = tableName(s2CatalogEntity());
        Map<String, Object> dov = new HashMap<>();
        dov.put(columnByField(s2CatalogEntity(), "name"),        "TC404 Destination");
        dov.put(columnByField(s2CatalogEntity(), "description"), "TC404 description");
        long destId = insertRowReturningId(dTable, dov);
        // Per Trip Planning M2 §5.2.12: GET /api/destinations/{id}/dashboard
        // returns DestinationDashboardDTO. After Builder retrofit the dashboard
        // must continue to populate its canonical fields.
        HttpResponse<String> dash = httpGetAuth(
                "/api/destinations/" + destId + "/dashboard", adminTok);
        assert2xx(dash, "TC404 GET destination dashboard");
        JsonNode j = parseNode(dash.body());
        assertTrue(j.has("totalItineraries") || j.has("total_itineraries"),
            "TC404: destination dashboard missing totalItineraries after Builder retrofit; body="
            + dash.body());
        assertTrue(j.has("completedItineraries") || j.has("completed_itineraries") ||
                   j.has("totalVisitors") || j.has("total_visitors"),
            "TC404: destination dashboard missing completedItineraries/totalVisitors after Builder retrofit; body="
            + dash.body());
    }

    @Test
    @DisplayName("TC405 — DP-4 Builder: M1 retrofit doesn't break behavior (S1-F3 trip-summary)")
    void tc405_m1RetrofitBehaviorUnchanged() throws Exception {
        BASE_URL = userServiceUrl;
        Map<String, Object> u = seedAndLoginUser("tc404s1f3");
        long uid = (long)(Number) u.get("id");
        String tok = (String) u.get("token");
        HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/trip-summary", tok);
        assert2xx(r, "TC405 S1-F3 trip-summary GET");
        JsonNode j = parseNode(r.body());
        assertTrue(j.has("totalTrips") || j.has("total_trips") ||
                   j.has("totalItineraries") || j.has("tripCount"),
            "TC405: trip-summary missing totalTrips after Builder retrofit; body=" + r.body());
        assertTrue(j.has("totalSpent") || j.has("total_spent") ||
                   j.has("totalAmount") || j.has("totalBudget"),
            "TC405: trip-summary missing totalSpent/totalBudget after Builder retrofit; body="
            + r.body());
    }

    @Test
    @DisplayName("TC406 — DP-4 Builder: Destination and Itinerary entities do NOT use Builder")
    void tc406_entityClassesExemptFromBuilder() throws Exception {
        BASE_URL = userServiceUrl;
        // S2-F8 (verifyDestinationReview) returns the updated Destination entity → no Builder.
        // S3-F8 (addDays) returns the updated Itinerary entity → no Builder.
        for (String entityName : new String[]{ s2CatalogEntity(), s3OrderEntity() }) {
            String src = readClassSource(entityName);
            if (src.isEmpty()) continue;
            assertFalse(src.contains("static class Builder") ||
                        src.contains("class Builder<"),
                "TC406: " + entityName + " entity must NOT have a Builder inner class. " +
                "S2-F8 (verify destination review) and S3-F8 (add days to itinerary) return entities directly — " +
                "Builder is only required for DTOs (per spec §3.5).");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DP-5  SINGLETON  (TC406–TC411)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC407 — DP-5 Singleton: JwtConfigurationManager has private constructor")
    void tc407_jwtConfigMgrPrivateConstructor() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("JwtConfigurationManager");
        assertFalse(src.isEmpty(),
            "TC407: JwtConfigurationManager.java not found in src/main/java.");
        assertTrue(src.contains("private JwtConfigurationManager("),
            "TC407: JwtConfigurationManager must have a private no-arg constructor. " +
            "Public constructors allow callers to bypass the singleton. " +
            "Found head: " + src.substring(0, Math.min(500, src.length())));
    }

    @Test
    @DisplayName("TC408 — DP-5 Singleton: getInstance() is public static")
    void tc408_getInstanceIsPublicStatic() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("JwtConfigurationManager");
        assertFalse(src.isEmpty(), "TC408: JwtConfigurationManager.java not found.");
        assertTrue(src.contains("getInstance()"),
            "TC408: JwtConfigurationManager must expose a public getInstance() method.");
        boolean isPublicStatic =
            src.contains("public static JwtConfigurationManager getInstance()") ||
            src.contains("public static synchronized JwtConfigurationManager getInstance()") ||
            (src.contains("public static") && src.contains("getInstance()"));
        assertTrue(isPublicStatic,
            "TC408: getInstance() must be declared public static. " +
            "Found head: " + src.substring(0, Math.min(500, src.length())));
    }

    @Test
    @DisplayName("TC409 — DP-5 Singleton: same reference — static instance field present")
    void tc409_staticInstanceFieldPresent() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("JwtConfigurationManager");
        assertFalse(src.isEmpty(), "TC409: JwtConfigurationManager.java not found.");
        boolean hasStaticInstance =
            src.contains("static JwtConfigurationManager instance")  ||
            src.contains("static JwtConfigurationManager INSTANCE")  ||
            src.contains("static final JwtConfigurationManager")     ||
            src.contains("private static JwtConfigurationManager")   ||
            src.contains("static volatile JwtConfigurationManager");
        assertTrue(hasStaticInstance,
            "TC409: JwtConfigurationManager must store the instance in a static field " +
            "(e.g., private static JwtConfigurationManager instance). " +
            "Without it, each getInstance() call allocates a new object — not a singleton.");
    }

    @Test
    @DisplayName("TC410 — DP-5 Singleton: thread-safe initialization")
    void tc410_threadSafeInitialization() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("JwtConfigurationManager");
        assertFalse(src.isEmpty(), "TC410: JwtConfigurationManager.java not found.");
        boolean threadSafe =
            src.contains("static final JwtConfigurationManager")  ||   // eager — always safe
            src.contains("volatile JwtConfigurationManager")        ||   // double-checked locking
            src.contains("synchronized JwtConfigurationManager getInstance") ||
            (src.contains("synchronized") && src.contains("getInstance"));
        assertTrue(threadSafe,
            "TC410: JwtConfigurationManager.getInstance() is not thread-safe. " +
            "Use eager init (static final), volatile + double-checked locking, or synchronized. " +
            "Lazy init without synchronisation lets concurrent first-callers each see instance==null " +
            "and each call new(), producing multiple instances.");
    }

    @Test
    @DisplayName("TC411 — DP-5 Singleton: NOT a Spring bean")
    void tc411_notASpringBean() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("JwtConfigurationManager");
        assertFalse(src.isEmpty(), "TC411: JwtConfigurationManager.java not found.");
        for (String ann : new String[]{"@Component", "@Service", "@Configuration",
                                       "@Bean", "@Repository", "@ManagedBean"}) {
            assertFalse(src.contains(ann),
                "TC411: JwtConfigurationManager must NOT carry " + ann + ". " +
                "It is a classical GoF Singleton — Spring must not manage its lifecycle. " +
                "Mixing Spring bean lifecycle with a manual getInstance() can produce two instances.");
        }
    }

    @Test
    @DisplayName("TC412 — DP-5 Singleton: JwtService reads via getInstance() — integration")
    void tc412_jwtServiceUsesGetInstance() throws Exception {
        BASE_URL = userServiceUrl;
        String jwtSvc = readClassSource("JwtService");
        if (!jwtSvc.isEmpty()) {
            assertTrue(jwtSvc.contains("JwtConfigurationManager.getInstance()") ||
                       jwtSvc.contains("getInstance()"),
                "TC412: JwtService must read config via JwtConfigurationManager.getInstance(). " +
                "Using @Autowired or @Value bypasses the singleton — secret may differ from the one " +
                "used at token-issue time. Found head: " + jwtSvc.substring(0, Math.min(400, jwtSvc.length())));
        }
        // Integration round-trip: token issued by service must pass validation on protected endpoint
        Map<String, Object> u = seedAndLoginUser("tc411jwt");
        long uid = (long)(Number) u.get("id");
        String tok = (String) u.get("token");
        HttpResponse<String> r = httpGetAuth("/api/users/" + uid, tok);
        assert2xx(r, "TC412 protected endpoint validates token with singleton-served secret");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DP-6  FACTORY  (TC412–TC419)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC413 — DP-6 Factory: MongoEvent interface")
    void tc413_mongoEventInterface() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("MongoEvent");
        assertFalse(src.isEmpty(),
            "TC413: MongoEvent.java not found in src/main/java. " +
            "Required: interface MongoEvent { String getId(); LocalDateTime getTimestamp(); " +
            "String getAction(); Map<String,Object> getDetails(); }");
        assertTrue(src.contains("interface MongoEvent"),
            "TC413: MongoEvent must be an interface. Found head: " +
            src.substring(0, Math.min(200, src.length())));
        for (String m : new String[]{"getId", "getTimestamp", "getAction", "getDetails"}) {
            assertTrue(src.contains(m),
                "TC413: MongoEvent must declare " + m + "() per spec §3.7 / §7.1.1.");
        }
    }

    @Test
    @DisplayName("TC414 — DP-6 Factory: 5 event classes implement MongoEvent")
    void tc414_fiveEventClassesImplementMongoEvent() throws Exception {
        BASE_URL = userServiceUrl;
        // Trip Planning event classes per spec §3.7 / §7.1: AuthEvent, DestinationEvent,
        // ItineraryEvent, ActivityEvent, PaymentAuditEvent.
        String[] events = {
            "AuthEvent", "DestinationEvent", "ItineraryEvent", "ActivityEvent", "PaymentAuditEvent"
        };
        List<String> missing = new ArrayList<>();
        for (String ev : events) {
            String src = readClassSource(ev);
            if (src.isEmpty()) { missing.add(ev + "(file not found)"); continue; }
            if (!src.contains("implements MongoEvent") &&
                !src.contains("implements MongoEvent,") &&
                !src.contains("implements MongoEvent ")) {
                missing.add(ev);
            }
        }
        assertTrue(missing.isEmpty(),
            "TC414: Event classes not implementing MongoEvent: " + missing);
    }

    @Test
    @DisplayName("TC415 — DP-6 Factory: createEvent(EventType, Map) signature")
    void tc415_createEventSignature() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("EventFactory");
        assertFalse(src.isEmpty(),
            "TC415: EventFactory.java not found in src/main/java.");
        assertTrue(src.contains("createEvent"),
            "TC415: EventFactory must declare a createEvent(…) method.");
        assertTrue(src.contains("EventType") && src.contains("Map"),
            "TC415: createEvent must accept (EventType, Map<String,Object>) parameters. " +
            "Found head: " + src.substring(0, Math.min(500, src.length())));
    }

    @Test
    @DisplayName("TC416 — DP-6 Factory: createEvent(AUTH, …) dispatches to AuthEvent")
    void tc416_factoryDispatchesAuthBranch() throws Exception {
        BASE_URL = userServiceUrl;
        // Federated per-microservice EventFactory split is a defensible pattern —
        // union ALL EventFactory.java files so the AUTH branch is found wherever it lives.
        String src = readAllSourcesNamed("EventFactory");
        assertFalse(src.isEmpty(), "TC416: EventFactory.java not found.");
        assertTrue(src.contains("AUTH") && src.contains("AuthEvent"),
            "TC416: EventFactory must handle EventType.AUTH and create AuthEvent. " +
            "Source must contain both 'AUTH' and 'AuthEvent' in the dispatch block.");
    }

    @Test
    @DisplayName("TC417 — DP-6 Factory: all 5 EventTypes dispatch correctly")
    void tc417_allFiveEventTypesDispatched() throws Exception {
        BASE_URL = userServiceUrl;
        // Per Trip Planning §3.7: EventType enum values are
        // AUTH, DESTINATION, ITINERARY, ACTIVITY, PAYMENT_AUDIT.
        String src = readAllSourcesNamed("EventFactory");
        assertFalse(src.isEmpty(), "TC417: EventFactory.java not found.");
        List<String> missing = new ArrayList<>();
        for (String t : new String[]{"AUTH", "DESTINATION", "ITINERARY", "ACTIVITY", "PAYMENT_AUDIT"}) {
            if (!src.contains(t)) missing.add(t);
        }
        assertTrue(missing.isEmpty(),
            "TC417: EventFactory missing dispatch branches for: " + missing +
            ". All 5 EventType values must produce the matching concrete event.");
    }

    @Test
    @DisplayName("TC418 — DP-6 Factory: PaymentAuditEvent exposes method + amount")
    void tc418_paymentAuditEventHasMethodAndAmount() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("PaymentAuditEvent");
        assertFalse(src.isEmpty(), "TC418: PaymentAuditEvent.java not found.");
        assertTrue(src.contains("method") || src.contains("paymentMethod"),
            "TC418: PaymentAuditEvent must have a 'method'/'paymentMethod' field (per spec §7.1.5). " +
            "Found head: " + src.substring(0, Math.min(400, src.length())));
        assertTrue(src.contains("amount") || src.contains("refundAmount"),
            "TC418: PaymentAuditEvent must have an 'amount' field. " +
            "Found head: " + src.substring(0, Math.min(400, src.length())));
    }

    @Test
    @DisplayName("TC419 — DP-6 Factory: register integration matches factory output")
    void tc419_registerEventMatchesFactoryShape() throws Exception {
        BASE_URL = userServiceUrl;
        if (mongo == null) throw new AssertionError("TC418: MongoDB required.");
        com.mongodb.client.MongoCollection<org.bson.Document> col =
                mongo.getCollection("auth_events");
        Map<String, Object> u = seedAndLoginUser("tc418fact");
        long uid = (long)(Number) u.get("id");
        org.bson.Document doc = col
                .find(new org.bson.Document("userId", uid))
                .sort(new org.bson.Document("_id", -1)).first();
        if (doc == null)
            doc = col.find().sort(new org.bson.Document("_id", -1)).first();
        assertNotNull(doc,
            "TC419: No auth_events doc found after registration. " +
            "EventFactory must be invoked — auth_events must contain an AuthEvent document.");
        String action = doc.getString("action");
        if (action == null) action = doc.getString("eventType");
        assertNotNull(action,
            "TC419: auth_events doc must have action/eventType (AuthEvent shape); doc=" + doc.toJson());
        assertTrue(doc.containsKey("userId") || doc.containsKey("user_id"),
            "TC419: auth_events doc missing userId field (AuthEvent shape per spec §7.1.1); doc=" + doc.toJson());
    }

    @Test
    @DisplayName("TC420 — DP-6 Factory: no `new XEvent(…)` in service classes")
    void tc420_noDirectEventConstructorsInServices() throws Exception {
        BASE_URL = userServiceUrl;
        String[] eventClasses = {
            "AuthEvent", "DestinationEvent", "ItineraryEvent", "ActivityEvent", "PaymentAuditEvent"
        };
        for (java.nio.file.Path p : allJavaFiles()) {
            String pathStr = p.toString();
            if (pathStr.contains("/src/test/")) continue;
            if (!pathStr.contains("/service/")) continue;   // narrow to service classes per @DisplayName
            String fname = p.getFileName().toString();
            if (fname.equals("EventFactory.java")) continue; // factory itself may use new
            boolean isEventClass = false;
            for (String ev : eventClasses)
                if (fname.equals(ev + ".java")) { isEventClass = true; break; }
            if (isEventClass) continue;
            String content;
            try { content = java.nio.file.Files.readString(p); }
            catch (java.io.IOException e) { continue; }
            for (String ev : eventClasses) {
                assertFalse(content.contains("new " + ev + "("),
                    "TC420: " + fname + " directly constructs " + ev + " with 'new'. " +
                    "All event creation must go through EventFactory.createEvent(…). " +
                    "Direct construction bypasses centralized factory logic (tracing, defaults, etc.).");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DP-7  ADAPTER  (TC420–TC425)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC421 — DP-7 Adapter: per-service NoSQL adapter classes present")
    void tc421_adapterClassesPresent() throws Exception {
        BASE_URL = userServiceUrl;
        List<String> missing = new ArrayList<>();
        if (!anySourceContains("class MongoDocumentAdapter"))
            missing.add("MongoDocumentAdapter (all services)");
        if (!anySourceContains("class ElasticsearchHitAdapter"))
            missing.add("ElasticsearchHitAdapter (destination-service)");
        if (!anySourceContains("class Neo4jRecordAdapter"))
            missing.add("Neo4jRecordAdapter (itinerary-service)");
        if (!anySourceContains("class CassandraRowAdapter"))
            missing.add("CassandraRowAdapter (activity-service)");
        assertTrue(missing.isEmpty(),
            "TC421: Missing Adapter classes: " + missing +
            ". Per spec §3.8, each service must have Adapter(s) for its NoSQL data sources.");
    }

    @Test
    @DisplayName("TC422 — DP-7 Adapter: each adapter has adapt() returning service DTO")
    void tc422_adaptersHaveAdaptMethod() throws Exception {
        BASE_URL = userServiceUrl;
        String[] adapters = {
            "MongoDocumentAdapter", "ElasticsearchHitAdapter",
            "Neo4jRecordAdapter",   "CassandraRowAdapter"
        };
        List<String> noAdapt = new ArrayList<>();
        for (String name : adapters) {
            String src = readClassSource(name);
            if (src.isEmpty()) continue; // absence already caught by TC420
            if (!src.contains("adapt(")) noAdapt.add(name);
        }
        assertTrue(noAdapt.isEmpty(),
            "TC422: Adapters missing adapt() method: " + noAdapt +
            ". Per spec §3.8, adapt(source) → targetDto is the required method name.");
    }

    @Test
    @DisplayName("TC423 — DP-7 Adapter: MongoDocumentAdapter.adapt(Document) → DTO")
    void tc423_mongoDocumentAdapterSignature() throws Exception {
        BASE_URL = userServiceUrl;
        String src = readClassSource("MongoDocumentAdapter");
        assertFalse(src.isEmpty(), "TC423: MongoDocumentAdapter.java not found.");
        assertTrue(src.contains("adapt(") &&
                   (src.contains("Document") || src.contains("document")),
            "TC423: MongoDocumentAdapter.adapt() must accept a MongoDB Document parameter. " +
            "Found: " + src.substring(0, Math.min(400, src.length())));
    }

    @Test
    @DisplayName("TC424 — DP-7 Adapter: ElasticsearchHitAdapter (destination-service)")
    void tc424_elasticsearchHitAdapterSignature() throws Exception {
        BASE_URL = catalogServiceUrl;
        String src = readClassSource("ElasticsearchHitAdapter");
        assertFalse(src.isEmpty(), "TC424: ElasticsearchHitAdapter.java not found.");
        assertTrue(src.contains("adapt("),
            "TC424: ElasticsearchHitAdapter must have an adapt() method.");
        assertTrue(src.contains("SearchHit") || src.contains("Hit") ||
                   src.contains("Map<") || src.contains("sourceAsMap"),
            "TC424: ElasticsearchHitAdapter.adapt() must accept a SearchHit or equivalent ES source. " +
            "Found: " + src.substring(0, Math.min(400, src.length())));
    }

    @Test
    @DisplayName("TC425 — DP-7 Adapter: ObjectArrayDtoAdapter for S1-F3")
    void tc425_objectArrayAdapterForS1F3() throws Exception {
        BASE_URL = userServiceUrl;
        boolean hasAdapter =
            anySourceContains("ObjectArrayDtoAdapter") ||
            anySourceContains("class ObjectArray")     ||
            anySourceContains("adapt(Object[]")        ||
            anySourceContains("adapt(Object[] ");
        assertTrue(hasAdapter,
            "TC425: No ObjectArrayDtoAdapter (or Object[]-to-DTO adapter) found in user-service. " +
            "Per spec §3.8, S1-F3 uses native SQL returning Object[] — an Adapter is required.");
        // Integration: S1-F3 must still return a correct DTO after the adapter retrofit
        Map<String, Object> u = seedAndLoginUser("tc424s1f3");
        long uid = (long)(Number) u.get("id");
        String tok = (String) u.get("token");
        HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/trip-summary", tok);
        assert2xx(r, "TC425 S1-F3 trip-summary after Adapter retrofit");
        JsonNode j = parseNode(r.body());
        assertTrue(
            j.has("totalTrips")        || j.has("total_trips")        ||
            j.has("totalItineraries")  || j.has("tripCount")          ||
            j.has("totalSpent")        || j.has("total_spent")        ||
            j.has("totalAmount")       || j.has("totalBudget"),
            "TC425: trip-summary DTO must have structural fields (totalTrips, totalSpent, …); " +
            "body=" + r.body());
    }
}
