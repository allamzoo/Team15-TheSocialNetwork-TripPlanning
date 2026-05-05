package com.team15.tripplanning.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team15.tripplanning.userservice.security.JwtService;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * §4.4 Caching retrofit — live integration tests.
 *
 * REQUIRES docker-compose stack fully running:
 *   user-service:8081, destination-service:8082, itinerary-service:8083,
 *   activity-service:8084, booking-service:8085, Redis:6379
 *
 * Run all:
 *   cd user-service && ../mvnw test -Dtest=CachingIntegrationTest -Dgroups=live-integration
 *
 * Run one scenario:
 *   ../mvnw test -Dtest="CachingIntegrationTest#a_*" -Dgroups=live-integration
 */
@Tag("live-integration")
@TestMethodOrder(MethodOrderer.MethodName.class)
class CachingIntegrationTest {

    // ── service URLs ──────────────────────────────────────────────────────────
    static final String USER_SVC = "http://localhost:8081";
    static final String DEST_SVC = "http://localhost:8082";
    static final String ITIN_SVC = "http://localhost:8083";
    static final String ACT_SVC  = "http://localhost:8084";
    static final String BOOK_SVC = "http://localhost:8085";
    static final String REDIS_URI = "redis://localhost:6379";

    // ── infrastructure ────────────────────────────────────────────────────────
    static HttpClient                             http;
    static RedisClient                            redisClient;
    static StatefulRedisConnection<String,String> redisConn;
    static RedisCommands<String,String>           redis;
    static final ObjectMapper mapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    // ── tokens / test-entity IDs ──────────────────────────────────────────────
    static String adminToken;   // generated in-process — ADMIN role, no DB row needed
    static String userToken;    // real registered-user JWT (user exists in DB)
    static Long   testUserId;
    static Long   testDestId;
    static Long   testItinId;
    static Long   testActId;
    static Long   testBookId;

    // ─────────────────────────────────────────────────────────────────────────
    // SETUP / TEARDOWN
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeAll
    static void setUpAll() throws Exception {
        http = HttpClient.newHttpClient();

        // Redis — skip all tests if unreachable
        try {
            redisClient = RedisClient.create(REDIS_URI);
            redisConn   = redisClient.connect();
            redis       = redisConn.sync();
            redis.ping();
        } catch (Exception e) {
            assumeTrue(false, "Redis not reachable at " + REDIS_URI);
        }

        // Admin token — generated directly using the same secret the services use
        adminToken = new JwtService().generateToken(1L, "admin@cache.test", "ADMIN");

        // Register a real test user (must exist in DB for user-service endpoints)
        long ts = System.currentTimeMillis() % 1_000_000_000L;
        String regBody = """
            {"name":"CacheTester","email":"cache%d@test.com",
             "password":"Test@1234","phone":"+201%09d"}
            """.formatted(ts, ts);
        HttpResponse<String> reg = post(USER_SVC + "/api/auth/register", regBody, "");
        assumeTrue(reg.statusCode() == 201,
                "user-service not running (register returned " + reg.statusCode() + ")");
        userToken  = mapper.readTree(reg.body()).get("token").asText();
        testUserId = new JwtService().extractUserId(userToken);

        // Seed test entities — best-effort; scenarios skip if 4xx
        testDestId = createEntity(DEST_SVC + "/api/destinations", adminToken, """
                {"name":"CacheIsland","country":"Greece","city":"Santorini",
                 "category":"BEACH","latitude":36.4,"longitude":25.4,"rating":4.8}""");

        if (testDestId != null) {
            testItinId = createEntity(ITIN_SVC + "/api/itineraries", userToken, """
                    {"userId":%d,"destinationId":%d,"title":"CacheTrip",
                     "startDate":"2026-07-01","endDate":"2026-07-10","budget":2000.0}
                    """.formatted(testUserId, testDestId));
        }
        if (testItinId != null) {
            testActId = createEntity(ACT_SVC + "/api/activities", userToken, """
                    {"itineraryId":%d,"name":"CacheHike","category":"OUTDOOR",
                     "scheduledTime":"2026-07-02T09:00:00","latitude":36.4,"longitude":25.4,
                     "metadata":{"cost":30.0,"duration":3.0}}""".formatted(testItinId));
            testBookId = createEntity(BOOK_SVC + "/api/bookings", userToken, """
                    {"userId":%d,"itineraryId":%d,"totalCost":1500.0,
                     "paymentMethod":"CREDIT_CARD"}""".formatted(testUserId, testItinId));
        }
    }

    @AfterAll
    static void tearDownAll() {
        try { if (redisConn   != null) redisConn.close();   } catch (Exception ignored) {}
        try { if (redisClient != null) redisClient.shutdown(); } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP HELPERS (all static — called from both @BeforeAll and test methods)
    // ─────────────────────────────────────────────────────────────────────────

    static HttpResponse<String> get(String url) throws Exception {
        return get(url, adminToken);
    }

    static HttpResponse<String> get(String url, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url)).GET();
        if (token != null && !token.isBlank())
            b.header("Authorization", "Bearer " + token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> post(String url, String body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null && !token.isBlank())
            b.header("Authorization", "Bearer " + token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> put(String url, String body) throws Exception {
        return http.send(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> delete(String url) throws Exception {
        return http.send(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + adminToken)
                .DELETE()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Returns elapsed milliseconds for one HTTP call. */
    @FunctionalInterface interface HttpCall { HttpResponse<String> call() throws Exception; }
    static long measure(HttpCall fn) throws Exception {
        long t = System.nanoTime();
        fn.call();
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REDIS HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    static void evict(String pattern) {
        List<String> keys = redis.keys(pattern);
        if (!keys.isEmpty()) redis.del(keys.toArray(new String[0]));
    }

    static void assertKeyExists(String pattern, String ctx) {
        assertFalse(redis.keys(pattern).isEmpty(),
                "Expected Redis key matching '" + pattern + "' after: " + ctx);
    }

    static void assertKeyAbsent(String pattern, String ctx) {
        assertTrue(redis.keys(pattern).isEmpty(),
                "Expected NO Redis key matching '" + pattern + "' after: " + ctx
                + " — found: " + redis.keys(pattern));
    }

    static Long createEntity(String url, String token, String body) {
        try {
            HttpResponse<String> r = post(url, body, token);
            if (r.statusCode() == 200 || r.statusCode() == 201)
                return mapper.readTree(r.body()).get("id").asLong();
        } catch (Exception ignored) {}
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── SCENARIO a ── 27 cached feature endpoints ─────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    record EP(String label, String url, String token, String keyPattern, String evictPattern) {}

    @Test
    @DisplayName("a) 27 cached feature endpoints: Redis key appears after 1st call; 2nd call is faster")
    void a_allCachedFeatureEndpoints_keyExistsAndSecondCallFaster() throws Exception {
        String s = "2025-01-01", e = "2026-12-31";
        long uid = testUserId != null ? testUserId : 1L;
        long did = testDestId != null ? testDestId : 1L;
        long iid = testItinId != null ? testItinId : 1L;
        long aid = testActId  != null ? testActId  : 1L;
        long bid = testBookId != null ? testBookId : 1L;

        List<EP> eps = List.of(
            // ── user-service (7) ──────────────────────────────────────────────
            new EP("S1-F1 all users",
                USER_SVC + "/api/users", adminToken,
                "s1-f1-users::S1::S1-F1::all", "s1-f1-users::*"),
            new EP("S1-F6 profile",
                USER_SVC + "/api/users/" + uid + "/profile", adminToken,
                "s1-f6-profile::S1::S1-F6::" + uid, "s1-f6-profile::S1::S1-F6::" + uid),
            new EP("S1-F3 trip summary",
                USER_SVC + "/api/users/" + uid + "/trip-summary", adminToken,
                "s1-f3-trip-summary::S1::S1-F3::" + uid, "s1-f3-trip-summary::*"),
            new EP("S1-F5 top travelers",
                USER_SVC + "/api/users/top-travelers?startDate=" + s + "&endDate=" + e + "&limit=5",
                adminToken,
                "s1-f5-top-travelers::S1::S1-F5::" + s + "::" + e + "::5",
                "s1-f5-top-travelers::*"),
            new EP("S1-F8 pref search",
                USER_SVC + "/api/users/preferences/search?key=theme&value=dark",
                adminToken,
                "s1-f8-pref-search::S1::S1-F8::theme::dark", "s1-f8-pref-search::*"),
            new EP("S1-F9 travel style",
                USER_SVC + "/api/users/preferences/travel-style?style=adventure&minTrips=0",
                adminToken,
                "s1-f9-travel-style::S1::S1-F9::adventure::0", "s1-f9-travel-style::*"),
            new EP("S1-F12 activity feed",
                USER_SVC + "/api/users/" + uid + "/activities?page=0&size=10",
                adminToken,
                "s1-f12-activity::S1::S1-F12::" + uid + "::0::10",
                "s1-f12-activity::S1::S1-F12::" + uid + "::*"),

            // ── destination-service (4) ───────────────────────────────────────
            new EP("S2-F1 dest search",
                DEST_SVC + "/api/destinations/search?minRating=3.0&maxRating=5.0",
                adminToken,
                "s2-dest-search::S2::S2-F1::*", "s2-dest-search::*"),
            new EP("S2-F3 top rated",
                DEST_SVC + "/api/destinations/top-rated?limit=5",
                adminToken,
                "s2-top-rated::S2::S2-F3::5", "s2-top-rated::*"),
            new EP("S2-F4 revenue summary",
                DEST_SVC + "/api/destinations/" + did + "/revenue-summary?startDate=" + s + "&endDate=" + e,
                adminToken,
                "s2-dest-revenue::S2::S2-F4::" + did + "::" + s + "::" + e,
                "s2-dest-revenue::*"),
            new EP("S2-F12 dashboard",
                DEST_SVC + "/api/destinations/" + did + "/dashboard",
                adminToken,
                "destination-service::S2-F12::" + did,
                "destination-service::S2-F12::" + did),

            // ── itinerary-service (5) ─────────────────────────────────────────
            new EP("S3 all itineraries",
                ITIN_SVC + "/api/itineraries", adminToken,
                "s3-itineraries::S3::all", "s3-itineraries::*"),
            new EP("S3-F3 cost estimate",
                ITIN_SVC + "/api/itineraries/cost-estimate?destinationId=" + did + "&numberOfDays=7&numberOfTravelers=2",
                adminToken,
                "s3-cost-estimate::S3::S3-F3::" + did + "::7::2",
                "s3-cost-estimate::*"),
            new EP("S3-F5 itinerary details",
                ITIN_SVC + "/api/itineraries/" + iid + "/details",
                adminToken,
                "s3-details::S3::S3-F5::" + iid,
                "s3-details::S3::S3-F5::" + iid),
            new EP("S3-F6 analytics",
                ITIN_SVC + "/api/itineraries/analytics?startDate=" + s + "&endDate=" + e,
                adminToken,
                "s3-analytics::S3::S3-F6::" + s + "::" + e,
                "s3-analytics::*"),
            new EP("S3-F12 recommendations",
                ITIN_SVC + "/api/itineraries/" + uid + "/recommendations?limit=5",
                adminToken,
                "s3-recommendations::S3::S3-F12::" + uid + "::5",
                "s3-recommendations::*"),

            // ── activity-service (6) ──────────────────────────────────────────
            new EP("S4 all activities",
                ACT_SVC + "/api/activities", adminToken,
                "s4-activities::S4::all", "s4-activities::*"),
            new EP("S4-F1 latest activity",
                ACT_SVC + "/api/activities/itinerary/" + iid + "/latest",
                adminToken,
                "s4-f1-latest::S4::S4-F1::" + iid,
                "s4-f1-latest::S4::S4-F1::" + iid),
            new EP("S4-F9 budget friendly",
                ACT_SVC + "/api/activities/budget-friendly?maxCost=100.0&sinceMinutes=60",
                adminToken,
                "s4-f9-budget::S4::S4-F9::100.0::60", "s4-f9-budget::*"),
            new EP("S4-F8 activity summary",
                ACT_SVC + "/api/activities/itinerary/" + iid + "/summary?startDate=" + s + "&endDate=" + e,
                adminToken,
                "s4-f8-summary::S4::S4-F8::" + iid + "::" + s + "::" + e,
                "s4-f8-summary::S4::S4-F8::" + iid + "::*"),
            new EP("S4-F10 analytics",
                ACT_SVC + "/api/activities/analytics?startDate=" + s + "&endDate=" + e,
                adminToken,
                "s4-f10-analytics::S4::S4-F10::*", "s4-f10-analytics::*"),
            new EP("S4-F12 timeline",
                ACT_SVC + "/api/activities/" + aid + "/timeline",
                adminToken,
                "s4-f12-timeline::S4::S4-F12::" + aid + "::*",
                "s4-f12-timeline::*"),

            // ── booking-service (5) ───────────────────────────────────────────
            new EP("S5-F3 booking summary",
                BOOK_SVC + "/api/users/" + uid + "/bookings/summary",
                adminToken,
                "s5-booking-summary::S5::S5-F3::" + uid,
                "s5-booking-summary::S5::S5-F3::" + uid),
            new EP("S5-F4 booking details",
                BOOK_SVC + "/api/bookings/" + bid + "/details",
                adminToken,
                "s5-booking-details::S5::S5-F4::" + bid,
                "s5-booking-details::S5::S5-F4::" + bid),
            new EP("S5-F5 top coupons",
                BOOK_SVC + "/api/coupons/top-used?limit=5",
                adminToken,
                "s5-top-coupons::S5::S5-F5::5", "s5-top-coupons::*"),
            new EP("S5-F6 revenue report",
                BOOK_SVC + "/api/bookings/analytics/revenue?startDate=" + s + "&endDate=" + e,
                adminToken,
                "s5-revenue-report::S5::S5-F6::" + s + "::" + e,
                "s5-revenue-report::*"),
            new EP("S5-F10 dest season",
                BOOK_SVC + "/api/bookings/analytics/destination-season?startDate=" + s + "&endDate=" + e,
                adminToken,
                "s5-destination-season::S5::S5-F10::*",
                "s5-destination-season::*")
        );

        assertEquals(27, eps.size(), "Must cover exactly 27 cached endpoints");

        List<String> failures = new ArrayList<>();
        for (EP ep : eps) {
            evict(ep.evictPattern());
            long t1 = measure(() -> get(ep.url(), ep.token()));

            List<String> found = redis.keys(ep.keyPattern());
            if (found.isEmpty()) {
                failures.add("[" + ep.label() + "] no Redis key matching '" + ep.keyPattern() + "'");
                continue;
            }

            long t2 = measure(() -> get(ep.url(), ep.token()));
            // Only enforce latency when the first call was clearly a DB round-trip (>20 ms)
            if (t1 > 20 && t2 >= t1) {
                failures.add("[" + ep.label() + "] 2nd call (" + t2 + "ms) not faster than 1st (" + t1 + "ms)");
            }
        }

        assertTrue(failures.isEmpty(), "Caching failures:\n" + String.join("\n", failures));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── SCENARIO b ── 10 by-ID cached endpoints ───────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("b) 10 CRUD GET-by-id endpoints: Redis key appears after first call")
    void b_byIdEndpoints_cacheKeyAppearsAfterFirstCall() throws Exception {
        long uid = testUserId != null ? testUserId : 1L;
        long did = testDestId != null ? testDestId : 1L;
        long iid = testItinId != null ? testItinId : 1L;
        long aid = testActId  != null ? testActId  : 1L;
        long bid = testBookId != null ? testBookId : 1L;
        String s = "2025-01-01", e = "2026-12-31";

        List<EP> byId = List.of(
            new EP("user profile",
                USER_SVC + "/api/users/" + uid + "/profile", adminToken,
                "s1-f6-profile::S1::S1-F6::" + uid,
                "s1-f6-profile::S1::S1-F6::" + uid),
            new EP("user trip-summary",
                USER_SVC + "/api/users/" + uid + "/trip-summary", adminToken,
                "s1-f3-trip-summary::S1::S1-F3::" + uid,
                "s1-f3-trip-summary::S1::S1-F3::" + uid),
            new EP("user activity-feed",
                USER_SVC + "/api/users/" + uid + "/activities?page=0&size=5", adminToken,
                "s1-f12-activity::S1::S1-F12::" + uid + "::0::5",
                "s1-f12-activity::S1::S1-F12::" + uid + "::*"),
            new EP("destination dashboard",
                DEST_SVC + "/api/destinations/" + did + "/dashboard", adminToken,
                "destination-service::S2-F12::" + did,
                "destination-service::S2-F12::" + did),
            new EP("destination revenue-summary",
                DEST_SVC + "/api/destinations/" + did + "/revenue-summary?startDate=" + s + "&endDate=" + e,
                adminToken,
                "s2-dest-revenue::S2::S2-F4::" + did + "::*",
                "s2-dest-revenue::S2::S2-F4::" + did + "::*"),
            new EP("itinerary details",
                ITIN_SVC + "/api/itineraries/" + iid + "/details", adminToken,
                "s3-details::S3::S3-F5::" + iid,
                "s3-details::S3::S3-F5::" + iid),
            new EP("latest activity for itinerary",
                ACT_SVC + "/api/activities/itinerary/" + iid + "/latest", adminToken,
                "s4-f1-latest::S4::S4-F1::" + iid,
                "s4-f1-latest::S4::S4-F1::" + iid),
            new EP("activity timeline",
                ACT_SVC + "/api/activities/" + aid + "/timeline", adminToken,
                "s4-f12-timeline::S4::S4-F12::" + aid + "::*",
                "s4-f12-timeline::*"),
            new EP("booking summary by user",
                BOOK_SVC + "/api/users/" + uid + "/bookings/summary", adminToken,
                "s5-booking-summary::S5::S5-F3::" + uid,
                "s5-booking-summary::S5::S5-F3::" + uid),
            new EP("booking details",
                BOOK_SVC + "/api/bookings/" + bid + "/details", adminToken,
                "s5-booking-details::S5::S5-F4::" + bid,
                "s5-booking-details::S5::S5-F4::" + bid)
        );

        assertEquals(10, byId.size());

        List<String> failures = new ArrayList<>();
        for (EP ep : byId) {
            evict(ep.evictPattern());
            HttpResponse<String> resp = get(ep.url(), ep.token());
            if (resp.statusCode() >= 400) {
                failures.add("[" + ep.label() + "] HTTP " + resp.statusCode() + " — entity may not exist");
                continue;
            }
            if (redis.keys(ep.keyPattern()).isEmpty())
                failures.add("[" + ep.label() + "] no Redis key matching '" + ep.keyPattern() + "'");
        }

        assertTrue(failures.isEmpty(), "By-ID cache failures:\n" + String.join("\n", failures));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── SCENARIO c ── list endpoint NOT cached ────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("c) GET /api/destinations (raw list) is NOT cached — no Redis key created")
    void c_listEndpointNotCached() throws Exception {
        evict("s2-destinations::*");
        HttpResponse<String> resp = get(DEST_SVC + "/api/destinations");
        assumeTrue(resp.statusCode() == 200, "destination-service not reachable");
        assertKeyAbsent("s2-destinations::*", "GET /api/destinations");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── SCENARIO d ── PUT evicts cache; next GET recomputes ────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("d) PUT /api/destinations/{id} evicts its cache key; next GET recomputes from PG")
    void d_putDestination_evictsCacheAndNextGetRecomputes() throws Exception {
        assumeTrue(testDestId != null, "testDestId not seeded — skipping");

        String url = DEST_SVC + "/api/destinations/" + testDestId + "/dashboard";
        String key = "destination-service::S2-F12::" + testDestId;

        // 1. prime
        evict(key);
        assumeTrue(get(url).statusCode() == 200, "Dashboard returned non-200");
        assertKeyExists(key, "first GET");

        // 2. mutate
        put(DEST_SVC + "/api/destinations/" + testDestId, """
                {"name":"Updated Island","country":"Greece","city":"Mykonos",
                 "category":"BEACH","latitude":37.4,"longitude":25.3,"rating":4.9}""");

        // 3. key must be gone
        assertKeyAbsent(key, "PUT /api/destinations/" + testDestId);

        // 4. recompute from PG
        HttpResponse<String> recomputed = get(url);
        assertEquals(200, recomputed.statusCode(), "GET after eviction must still return 200");
        assertKeyExists(key, "GET after eviction (recomputed)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── SCENARIO e ── wildcard eviction on preferences update ────────────────
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("e) PUT /api/users/{id}/preferences wildcard-evicts profile & pref-search caches")
    void e_putPreferences_wildcardEvictsRelatedCaches() throws Exception {
        assumeTrue(testUserId != null, "testUserId not available");

        String profileUrl = USER_SVC + "/api/users/" + testUserId + "/profile";
        String profileKey = "s1-f6-profile::S1::S1-F6::" + testUserId;
        String prefKey    = "s1-f8-pref-search::*";

        // 1. prime both caches
        evict(profileKey);
        evict("s1-f8-pref-search::*");
        assumeTrue(get(profileUrl).statusCode() == 200, "profile endpoint not reachable");
        get(USER_SVC + "/api/users/preferences/search?key=theme&value=dark");

        assertKeyExists(profileKey, "prime profile");
        assertKeyExists("s1-f8-pref-search::S1::S1-F8::theme::dark", "prime pref-search");

        // 2. PUT preferences triggers wildcard eviction
        put(USER_SVC + "/api/users/" + testUserId + "/preferences",
            """
            {"theme":"dark","language":"ar","notifications":"none"}
            """);

        // 3. both must be evicted
        assertKeyAbsent(profileKey, "PUT preferences → profile evicted");
        assertKeyAbsent("s1-f8-pref-search::*", "PUT preferences → pref-search evicted");

        // 4. GET still works (recomputes from DB)
        assertEquals(200, get(profileUrl).statusCode(),
                "profile must return 200 after cache eviction");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── SCENARIO f ── DELETE evicts cache ─────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("f) DELETE /api/destinations/{id} removes its dashboard cache key")
    void f_deleteDestination_evictsCacheKey() throws Exception {
        // Create a throwaway destination so we don't break other scenarios
        Long delId = createEntity(DEST_SVC + "/api/destinations", adminToken, """
                {"name":"ToDelete","country":"Italy","city":"Rome",
                 "category":"CULTURAL","latitude":41.9,"longitude":12.5,"rating":4.0}""");
        assumeTrue(delId != null, "Could not create destination for delete test");

        String url = DEST_SVC + "/api/destinations/" + delId + "/dashboard";
        String key = "destination-service::S2-F12::" + delId;

        // 1. prime
        evict(key);
        HttpResponse<String> prime = get(url);
        assumeTrue(prime.statusCode() == 200, "dashboard returned " + prime.statusCode());
        assertKeyExists(key, "GET before DELETE");

        // 2. delete
        HttpResponse<String> del = delete(DEST_SVC + "/api/destinations/" + delId);
        assertTrue(del.statusCode() == 200 || del.statusCode() == 204,
                "DELETE must succeed, got " + del.statusCode());

        // 3. key must be gone
        assertKeyAbsent(key, "DELETE /api/destinations/" + delId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── SCENARIO g ── graceful degradation when Redis is down ─────────────────
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("g) Redis down → cached endpoint still returns 200 from PostgreSQL")
    void g_redisDown_endpointStillServesDataFromPostgres() throws Exception {
        String container = findRedisContainerName();
        assumeTrue(container != null, "Redis container not found via docker ps — skipping");

        String url = USER_SVC + "/api/users";
        try {
            // Stop Redis
            runDocker("stop", container);
            Thread.sleep(2000);

            // Endpoint must still return 200 (PG fallback)
            HttpResponse<String> resp = get(url);
            assertEquals(200, resp.statusCode(),
                    "Service must fall back to PostgreSQL when Redis is down; got "
                    + resp.statusCode() + " body=" + resp.body().substring(0, Math.min(200, resp.body().length())));
            assertFalse(resp.body().isBlank(), "Response body must not be empty");

        } finally {
            // Always restart Redis — failure here would break subsequent tests
            runDocker("start", container);
            Thread.sleep(3000);
            try {
                redisConn.close();
                redisConn = redisClient.connect();
                redis     = redisConn.sync();
                redis.ping();
            } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── SCENARIO h ── TTL auto-eviction (disabled — takes 5+ minutes) ─────────
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Disabled("Takes 5+ minutes — run manually: ../mvnw test -Dtest='CachingIntegrationTest#h_*' -Dgroups=live-integration")
    @DisplayName("h) S1-F1 key (TTL 5 min) is auto-evicted; next call recomputes from DB")
    void h_ttlAutoEviction_keyGoneAfterFiveMinutes() throws Exception {
        String url = USER_SVC + "/api/users";
        String key = "s1-f1-users::S1::S1-F1::all";

        evict("s1-f1-users::*");
        assumeTrue(get(url).statusCode() == 200, "user-service not reachable");
        assertKeyExists(key, "first GET");

        // Assert Redis has set a TTL ≤ 300 s
        long ttl = redis.ttl(key);
        assertTrue(ttl > 0 && ttl <= 300,
                "TTL must be set and ≤ 300 s (5 min); actual: " + ttl + "s");
        System.out.println("[h] TTL=" + ttl + "s — waiting " + (ttl + 5) + "s for auto-eviction…");

        Thread.sleep((ttl + 5) * 1000L);

        assertKeyAbsent(key, "after TTL expiry (" + ttl + "s)");

        // Recomputes from DB — key reappears
        assertEquals(200, get(url).statusCode(), "Must return 200 after TTL eviction");
        assertKeyExists(key, "GET after TTL eviction (recomputed from DB)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DOCKER HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static String findRedisContainerName() {
        try {
            Process p = new ProcessBuilder(
                    "docker", "ps", "--filter", "name=redis", "--format", "{{.Names}}")
                    .start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(5, TimeUnit.SECONDS);
            return out.isBlank() ? null : out.split("\n")[0].trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static void runDocker(String command, String container) throws Exception {
        new ProcessBuilder("docker", command, container)
                .start().waitFor(10, TimeUnit.SECONDS);
    }
}
