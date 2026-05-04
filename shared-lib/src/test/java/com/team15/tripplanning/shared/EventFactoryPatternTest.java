package com.team15.tripplanning.shared;

import com.team15.tripplanning.shared.event.EventFactory;
import com.team15.tripplanning.shared.event.EventType;
import com.team15.tripplanning.shared.mongo.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Factory pattern verification for EventFactory + MongoEvent hierarchy.
 *
 * Scenarios:
 *   a) MongoEvent interface declares getId, getTimestamp, getAction, getDetails
 *   b) All 5 event classes implement MongoEvent
 *   c) EventFactory has createEvent(EventType, Map<String,Object>) — public static, returns MongoEvent
 *   d) createEvent(AUTH, params) returns AuthEvent with fields matching params
 *   e) All 5 EventType values produce the correct concrete class
 *   f) createEvent(PAYMENT_AUDIT, params) carries method and amount in its details
 *   h) Source scan: no *Service.java file instantiates event classes directly (bypass check)
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class EventFactoryPatternTest {

    // ── a ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("a) MongoEvent interface declares getId, getTimestamp, getAction, getDetails")
    void a_mongoEventInterfaceHasRequiredMethods() throws Exception {
        Class<?> iface = Class.forName("com.team15.tripplanning.shared.mongo.MongoEvent");

        assertTrue(iface.isInterface(),
                "MongoEvent must be declared as an interface");

        assertNotNull(iface.getMethod("getId"),
                "MongoEvent must declare getId()");
        assertNotNull(iface.getMethod("getTimestamp"),
                "MongoEvent must declare getTimestamp()");
        assertNotNull(iface.getMethod("getAction"),
                "MongoEvent must declare getAction()");
        assertNotNull(iface.getMethod("getDetails"),
                "MongoEvent must declare getDetails()");
    }

    // ── b ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("b) All 5 event classes implement MongoEvent")
    void b_allFiveEventClassesImplementMongoEvent() throws Exception {
        Class<?> mongoEvent = Class.forName("com.team15.tripplanning.shared.mongo.MongoEvent");

        String[] classNames = {
                "com.team15.tripplanning.shared.mongo.AuthEvent",
                "com.team15.tripplanning.shared.mongo.DestinationEvent",
                "com.team15.tripplanning.shared.mongo.ItineraryEvent",
                "com.team15.tripplanning.shared.mongo.ActivityEvent",
                "com.team15.tripplanning.shared.mongo.PaymentAuditEvent"
        };

        for (String name : classNames) {
            Class<?> clazz = Class.forName(name);
            assertTrue(mongoEvent.isAssignableFrom(clazz),
                    clazz.getSimpleName() + " must implement MongoEvent");
            assertFalse(clazz.isInterface(),
                    clazz.getSimpleName() + " must be a concrete class, not an interface");
        }
    }

    // ── c ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("c) EventFactory declares a public static createEvent(EventType, Map<String,Object>) returning MongoEvent")
    void c_eventFactoryHasCreateEventMethod() throws Exception {
        Class<?> factory = Class.forName("com.team15.tripplanning.shared.event.EventFactory");

        Method m = factory.getDeclaredMethod("createEvent", EventType.class, Map.class);

        assertTrue(Modifier.isPublic(m.getModifiers()),
                "createEvent must be public");
        assertTrue(Modifier.isStatic(m.getModifiers()),
                "createEvent must be static");
        assertEquals(MongoEvent.class, m.getReturnType(),
                "createEvent must return MongoEvent");
    }

    // ── d ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("d) createEvent(AUTH, params) returns AuthEvent with fields matching params")
    void d_createEventAuth_returnsAuthEventWithMatchingFields() {
        Map<String, Object> params = new HashMap<>();
        params.put("action", "REGISTERED");
        params.put("userId", 42L);

        MongoEvent event = EventFactory.createEvent(EventType.AUTH, params);

        assertInstanceOf(AuthEvent.class, event,
                "EventType.AUTH must produce an AuthEvent");
        assertEquals("REGISTERED", event.getAction(),
                "action must match the params value");
        assertNotNull(event.getId(),
                "id must be auto-generated (not null)");
        assertNotNull(event.getTimestamp(),
                "timestamp must be auto-set (not null)");
        assertEquals(42L, event.getDetails().get("userId"),
                "userId must be present in the details map");
    }

    // ── e ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("e) EventFactory produces the correct concrete class for each of the 5 EventType values")
    void e_allFiveEventTypes_returnCorrectConcreteClass() {
        record Case(EventType type, String action, Class<?> expectedClass) {}

        var cases = List.of(
                new Case(EventType.AUTH,          "REGISTERED",    AuthEvent.class),
                new Case(EventType.DESTINATION,   "DEST_CREATED",  DestinationEvent.class),
                new Case(EventType.ITINERARY,     "ITIN_CREATED",  ItineraryEvent.class),
                new Case(EventType.ACTIVITY,      "ACT_CREATED",   ActivityEvent.class),
                new Case(EventType.PAYMENT_AUDIT, "PAYMENT_MADE",  PaymentAuditEvent.class)
        );

        for (var c : cases) {
            Map<String, Object> params = Map.of("action", c.action());
            MongoEvent event = EventFactory.createEvent(c.type(), params);

            assertInstanceOf(c.expectedClass(), event,
                    "EventType." + c.type() + " must produce " + c.expectedClass().getSimpleName());
            assertEquals(c.action(), event.getAction(),
                    "action must match for EventType." + c.type());
            assertNotNull(event.getId(),
                    "id must be set for EventType." + c.type());
            assertNotNull(event.getTimestamp(),
                    "timestamp must be set for EventType." + c.type());
        }
    }

    // ── f ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("f) createEvent(PAYMENT_AUDIT,...) carries method and amount as service-specific fields in details")
    void f_paymentAuditEvent_exposesMethodAndAmountFields() {
        Map<String, Object> params = new HashMap<>();
        params.put("action",    "PAYMENT_MADE");
        params.put("bookingId", 5L);
        params.put("userId",    99L);
        params.put("method",    "CREDIT_CARD");
        params.put("amount",    250.75);

        MongoEvent event = EventFactory.createEvent(EventType.PAYMENT_AUDIT, params);

        assertInstanceOf(PaymentAuditEvent.class, event,
                "EventType.PAYMENT_AUDIT must return a PaymentAuditEvent");

        // The service-specific fields (method, amount) are carried in details.
        // Each service's @Document class maps these to dedicated getMethod()/getAmount() fields.
        assertEquals("CREDIT_CARD", event.getDetails().get("method"),
                "method must be present in the event details");
        assertEquals(250.75, (Double) event.getDetails().get("amount"), 0.001,
                "amount must be present in the event details");
        assertEquals(5L, event.getDetails().get("bookingId"),
                "bookingId must be present in the event details");
        assertEquals(99L, event.getDetails().get("userId"),
                "userId must be present in the event details");
    }

    // ── h ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("h) No *Service.java class instantiates event classes directly — all construction goes through EventFactory")
    void h_noDirectEventInstantiationInServiceClasses() throws Exception {
        // Maven sets user.dir to the module directory (shared-lib/).
        // Parent directory is the tripplanning project root.
        Path projectRoot = Path.of(System.getProperty("user.dir")).getParent();

        String[] serviceModules = {
                "user-service",
                "destination-service",
                "itinerary-service",
                "activity-service",
                "booking-service"
        };

        // These patterns represent direct construction that bypasses the factory
        String[] forbidden = {
                "new AuthEvent(",
                "new DestinationEvent(",
                "new ItineraryEvent(",
                "new ActivityEvent(",
                "new PaymentAuditEvent("
        };

        List<String> violations = new ArrayList<>();

        for (String module : serviceModules) {
            Path srcDir = projectRoot.resolve(module).resolve("src/main/java");
            if (!Files.exists(srcDir)) continue;

            try (var stream = Files.walk(srcDir)) {
                stream
                    .filter(p -> p.toString().endsWith("Service.java"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            for (String pattern : forbidden) {
                                if (content.contains(pattern)) {
                                    violations.add("[" + module + "] "
                                            + p.getFileName()
                                            + " → direct instantiation: " + pattern);
                                }
                            }
                        } catch (Exception ignored) {}
                    });
            }
        }

        assertTrue(violations.isEmpty(),
                "Found direct event class instantiation that bypasses EventFactory:\n"
                        + String.join("\n", violations));
    }
}
