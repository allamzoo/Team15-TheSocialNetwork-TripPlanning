package com.team15.tripplanning.userservice;

import com.team15.tripplanning.userservice.security.JwtConfigurationManager;
import com.team15.tripplanning.userservice.security.JwtService;
import org.junit.jupiter.api.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Singleton pattern verification for JwtConfigurationManager.
 *
 * Scenarios:
 *   a) Exactly one private constructor — via reflection
 *   b) getInstance() is public static returning JwtConfigurationManager — via reflection
 *   c) Two sequential calls return reference-equal instances (ref1 == ref2)
 *   d) 10 parallel threads all receive the same reference — thread-safety under contention
 *   e) Class carries no Spring stereotype annotation (@Component, @Service, etc.)
 *   f) JwtService (reads config via getInstance()) correctly issues and validates a token
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@SuppressWarnings("unchecked")
class JwtConfigurationManagerSingletonTest {

    /** Resets the private static volatile field so test d can race to create it. */
    private static void resetSingleton() throws Exception {
        Field field = JwtConfigurationManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    // ── a ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("a) JwtConfigurationManager declares exactly one constructor and it is private")
    void a_exactlyOnePrivateConstructor() throws Exception {
        Class<?> clazz = Class.forName(
                "com.team15.tripplanning.userservice.security.JwtConfigurationManager");

        Constructor<?>[] ctors = clazz.getDeclaredConstructors();

        assertEquals(1, ctors.length,
                "JwtConfigurationManager must declare exactly one constructor");

        assertTrue(Modifier.isPrivate(ctors[0].getModifiers()),
                "The constructor must have private access");
    }

    // ── b ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("b) getInstance() is a public static method returning JwtConfigurationManager")
    void b_getInstanceIsPublicStaticReturningCorrectType() throws Exception {
        Class<?> clazz = Class.forName(
                "com.team15.tripplanning.userservice.security.JwtConfigurationManager");

        Method getInstance = clazz.getDeclaredMethod("getInstance");

        assertTrue(Modifier.isPublic(getInstance.getModifiers()),
                "getInstance() must be public");

        assertTrue(Modifier.isStatic(getInstance.getModifiers()),
                "getInstance() must be static");

        assertEquals(clazz, getInstance.getReturnType(),
                "getInstance() must return JwtConfigurationManager");
    }

    // ── c ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("c) Two sequential calls to getInstance() return the same reference (ref1 == ref2)")
    void c_sequentialCallsReturnSameReference() {
        JwtConfigurationManager ref1 = JwtConfigurationManager.getInstance();
        JwtConfigurationManager ref2 = JwtConfigurationManager.getInstance();

        assertSame(ref1, ref2,
                "getInstance() must return reference-equal instances on every call");
    }

    // ── d ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("d) 10 parallel threads all receive the same singleton reference")
    void d_tenParallelThreadsReturnSameReference() throws Exception {
        // Reset so all threads compete to initialise the instance
        resetSingleton();

        int threadCount = 10;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        List<JwtConfigurationManager> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    startGate.await();                         // all threads block here
                    results.add(JwtConfigurationManager.getInstance());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        startGate.countDown();                                 // release all at once

        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        assertTrue(finished, "All 10 threads must finish within 5 seconds");

        assertEquals(threadCount, results.size(),
                "All 10 threads must have stored a result");

        JwtConfigurationManager expected = results.get(0);
        for (JwtConfigurationManager ref : results) {
            assertSame(expected, ref,
                    "Every thread must receive the exact same singleton reference");
        }
    }

    // ── e ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("e) JwtConfigurationManager carries no Spring stereotype annotation")
    void e_noSpringStereotypeAnnotations() throws Exception {
        Class<?> clazz = Class.forName(
                "com.team15.tripplanning.userservice.security.JwtConfigurationManager");

        String[] forbidden = {
                "org.springframework.stereotype.Component",
                "org.springframework.stereotype.Service",
                "org.springframework.context.annotation.Configuration",
                "org.springframework.stereotype.Repository",
                "org.springframework.stereotype.Controller",
                "org.springframework.web.bind.annotation.RestController",
                "org.springframework.boot.context.properties.ConfigurationProperties"
        };

        for (String annotationFqn : forbidden) {
            try {
                Class<? extends Annotation> ann =
                        (Class<? extends Annotation>) Class.forName(annotationFqn);
                String shortName = annotationFqn.substring(annotationFqn.lastIndexOf('.') + 1);
                assertFalse(clazz.isAnnotationPresent(ann),
                        "JwtConfigurationManager must NOT be annotated with @" + shortName
                                + " — it is a hand-rolled Singleton, not a Spring-managed bean");
            } catch (ClassNotFoundException ignored) {
                // annotation not on the test classpath → cannot be present → pass
            }
        }
    }

    // ── f ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("f) JwtService reads the secret via getInstance() and correctly issues/validates a token")
    void f_jwtServiceReadsSecretFromConfigManagerAndValidatesToken() {
        // JwtService has a no-arg constructor that calls JwtConfigurationManager.getInstance()
        JwtService jwtService = new JwtService();

        // Confirm getInstance() returns a usable secret (the same one JwtService used)
        String secret = JwtConfigurationManager.getInstance().getSecretKey();
        assertNotNull(secret, "JwtConfigurationManager must provide a non-null secret");
        assertFalse(secret.isBlank(), "JwtConfigurationManager must provide a non-blank secret");

        // Issue a token
        String token = jwtService.generateToken(7L, "singleton@test.com", "ADMIN");
        assertNotNull(token, "generateToken() must not return null");
        assertFalse(token.isBlank(), "generateToken() must not return a blank string");

        // The token must be valid against the same key
        assertTrue(jwtService.isTokenValid(token),
                "Token issued by JwtService must pass its own validation");

        // Claims must exactly match the values used when issuing
        assertEquals("singleton@test.com", jwtService.extractEmail(token),
                "Email claim must match");
        assertEquals(7L, jwtService.extractUserId(token),
                "userId claim must match");
        assertEquals("ADMIN", jwtService.extractRole(token),
                "role claim must match");

        // A tampered token must be rejected
        assertFalse(jwtService.isTokenValid(token + "x"),
                "A tampered token must fail validation");

        // expirationMs from config must be positive
        long expMs = JwtConfigurationManager.getInstance().getExpirationMs();
        assertTrue(expMs > 0,
                "JwtConfigurationManager.getExpirationMs() must be a positive value, got: " + expMs);
    }
}
