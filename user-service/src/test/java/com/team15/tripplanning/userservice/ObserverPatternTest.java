package com.team15.tripplanning.userservice;

import com.team15.tripplanning.userservice.document.AuthEvent;
import com.team15.tripplanning.userservice.dto.LoginRequest;
import com.team15.tripplanning.userservice.dto.RegisterRequest;
import com.team15.tripplanning.userservice.model.Role;
import com.team15.tripplanning.userservice.model.Status;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.observer.EntityObserver;
import com.team15.tripplanning.userservice.observer.MongoEventLogger;
import com.team15.tripplanning.userservice.repository.AuthEventRepository;
import com.team15.tripplanning.userservice.repository.UserRepository;
import com.team15.tripplanning.userservice.security.JwtService;
import com.team15.tripplanning.userservice.service.AuthService;
import com.team15.tripplanning.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.timeout;

/**
 * STEP 7 — Observer + Factory Pattern tests.
 *
 * Covers all 7 scenarios:
 *   a) EntityObserver interface exists with onEvent(String, Object) — via reflection
 *   b) MongoEventLogger implements EntityObserver — via reflection
 *   c) No @EventListener in service methods; observer chain infrastructure present — static analysis
 *   d) POST /register → REGISTERED document in auth_events with matching userId
 *   e) POST /login   → LOGGED_IN document added
 *   f) PUT /preferences → USER_UPDATED event through observer chain (retrofit path)
 *   g) Unregister all observers → no MongoDB write (proves logging goes through the chain)
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class ObserverPatternTest {

    // ── mocks ──────────────────────────────────────────────────────────────────
    private AuthEventRepository authEventRepository;
    private UserRepository      userRepository;
    private PasswordEncoder     passwordEncoder;
    private JwtService          jwtService;
    private RedisTemplate       redisTemplate;

    // ── real objects under test ────────────────────────────────────────────────
    private MongoEventLogger mongoEventLogger;
    private UserService      userService;
    private AuthService      authService;

    @BeforeEach
    void setUp() {
        authEventRepository = mock(AuthEventRepository.class);
        userRepository      = mock(UserRepository.class);
        passwordEncoder     = mock(PasswordEncoder.class);
        jwtService          = mock(JwtService.class);
        redisTemplate       = mock(RedisTemplate.class);

        // Real MongoEventLogger backed by mocked repo — lets us capture save() calls
        mongoEventLogger = new MongoEventLogger(authEventRepository);

        // Real service wired with real MongoEventLogger → observer chain is live
        userService = new UserService(
                userRepository, mongoEventLogger, passwordEncoder,
                redisTemplate, authEventRepository);

        authService = new AuthService(
                userRepository, passwordEncoder, jwtService, mongoEventLogger);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // a) Via reflection: EntityObserver is an interface with onEvent(String, Object)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("a) EntityObserver is an interface with onEvent(String, Object)")
    void a_entityObserverIsInterfaceWithOnEventMethod() throws Exception {
        Class<?> iface = Class.forName(
                "com.team15.tripplanning.userservice.observer.EntityObserver");

        assertTrue(iface.isInterface(),
                "EntityObserver must be declared as an interface");

        Method onEvent = iface.getMethod("onEvent", String.class, Object.class);
        assertNotNull(onEvent,
                "EntityObserver must declare onEvent(String, Object)");

        // Every method on an interface is implicitly abstract
        assertTrue(Modifier.isPublic(onEvent.getModifiers()),
                "onEvent must be public");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // b) Via reflection: MongoEventLogger implements EntityObserver
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("b) MongoEventLogger implements EntityObserver")
    void b_mongoEventLoggerImplementsEntityObserver() throws Exception {
        Class<?> loggerClass    = Class.forName(
                "com.team15.tripplanning.userservice.observer.MongoEventLogger");
        Class<?> observerIface  = Class.forName(
                "com.team15.tripplanning.userservice.observer.EntityObserver");

        assertTrue(observerIface.isAssignableFrom(loggerClass),
                "MongoEventLogger must implement EntityObserver");

        // Confirm the implementation is concrete (not abstract)
        assertFalse(Modifier.isAbstract(loggerClass.getModifiers()),
                "MongoEventLogger must be a concrete class, not abstract");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // c) Static analysis: no @EventListener in service methods;
    //    observer-chain scaffolding (observers list, register, unregister,
    //    notifyObservers) must be present in UserService.
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("c) No @EventListener in service classes; GoF observer chain present in UserService")
    void c_noEventListenerAnnotation_andObserverChainPresent() throws Exception {
        // --- Static analysis: no @EventListener allowed in service methods ---
        String[] serviceClassNames = {
                "com.team15.tripplanning.userservice.service.UserService",
                "com.team15.tripplanning.userservice.service.AuthService"
        };
        for (String className : serviceClassNames) {
            Class<?> svc = Class.forName(className);
            for (Method m : svc.getDeclaredMethods()) {
                assertFalse(m.isAnnotationPresent(EventListener.class),
                        "Method [" + m.getName() + "] in " + className
                                + " must NOT use @EventListener for MongoDB writes — "
                                + "all event logging must flow through the GoF Observer chain.");
            }
        }

        // --- Observer-chain infrastructure must exist in UserService ---
        Class<?> userServiceClass = Class.forName(
                "com.team15.tripplanning.userservice.service.UserService");
        Class<?> observerIface    = Class.forName(
                "com.team15.tripplanning.userservice.observer.EntityObserver");

        // Field: List<EntityObserver> observers
        Field observersField = userServiceClass.getDeclaredField("observers");
        observersField.setAccessible(true);
        assertTrue(List.class.isAssignableFrom(observersField.getType()),
                "UserService must have an 'observers' field of type List<EntityObserver>");

        // Method: register(EntityObserver)
        Method registerMethod = userServiceClass.getDeclaredMethod("register", observerIface);
        assertNotNull(registerMethod,
                "UserService must expose register(EntityObserver)");

        // Method: unregister(EntityObserver)
        Method unregisterMethod = userServiceClass.getDeclaredMethod("unregister", observerIface);
        assertNotNull(unregisterMethod,
                "UserService must expose unregister(EntityObserver)");

        // Method: notifyObservers(String, Object) — private is acceptable
        Method notifyMethod = userServiceClass.getDeclaredMethod(
                "notifyObservers", String.class, Object.class);
        assertNotNull(notifyMethod,
                "UserService must have a notifyObservers(String, Object) method");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // d) Register a fresh user → exactly one REGISTERED document in auth_events
    //    with matching userId and a non-null timestamp
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("d) register() writes exactly one REGISTERED document with matching userId")
    void d_register_writesRegisteredEventToMongo() {
        // Arrange
        User savedUser = new User();
        savedUser.setId(42L);
        savedUser.setEmail("alice@example.com");
        savedUser.setPassword("encoded_password");
        savedUser.setRole(Role.TRAVELER);
        savedUser.setStatus(Status.ACTIVE);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0123456789")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(42L, "alice@example.com", "TRAVELER"))
                .thenReturn("mock_token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com",
                "secret", "0123456789");

        // Act
        authService.register(req);

        // Assert: exactly one save, with action=REGISTERED and correct userId
        // timeout() needed because MongoEventLogger writes via CompletableFuture.runAsync()
        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository, timeout(2000).times(1)).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertEquals("REGISTERED", saved.getAction(),
                "action must be REGISTERED");
        assertEquals(42L, saved.getUserId(),
                "userId must match the newly registered user");
        assertNotNull(saved.getTimestamp(),
                "timestamp must not be null");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // e) Login as that user → exactly one LOGGED_IN document added
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("e) login() writes exactly one LOGGED_IN document with matching userId")
    void e_login_writesLoggedInEventToMongo() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(42L);
        existingUser.setEmail("alice@example.com");
        existingUser.setPassword("encoded_password");
        existingUser.setRole(Role.TRAVELER);
        existingUser.setStatus(Status.ACTIVE);

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("secret", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken(42L, "alice@example.com", "TRAVELER"))
                .thenReturn("mock_token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        LoginRequest req = new LoginRequest("alice@example.com", "secret");

        // Act
        authService.login(req);

        // Assert — timeout() because MongoEventLogger writes via CompletableFuture.runAsync()
        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository, timeout(2000).times(1)).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertEquals("LOGGED_IN", saved.getAction(),
                "action must be LOGGED_IN");
        assertEquals(42L, saved.getUserId(),
                "userId must match the logged-in user");
        assertNotNull(saved.getTimestamp(),
                "timestamp must not be null");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // f) M1 write (PUT preferences) → USER_UPDATED event written via observer chain
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("f) mergePreferences() writes USER_UPDATED event through the observer chain")
    void f_updatePreferences_writesUserUpdatedEventToMongo() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("bob@example.com");
        existingUser.setPreferences(new HashMap<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        // redisTemplate.keys() returns null by default → deleteWildcard is a no-op

        Map<String, Object> newPrefs = Map.of("theme", "dark", "language", "en");

        // Act
        userService.mergePreferences(1L, newPrefs);

        // Assert: observer chain fired → exactly one AUTH event saved with USER_UPDATED
        // timeout() because MongoEventLogger writes via CompletableFuture.runAsync()
        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository, timeout(2000).times(1)).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertEquals("USER_UPDATED", saved.getAction(),
                "mergePreferences must emit USER_UPDATED through the observer chain");
        assertEquals(1L, saved.getUserId(),
                "userId must match the user whose preferences were updated");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // g) Unregister all observers → repeat the same write → no MongoDB document
    //    (proves the logging path goes through the observer chain, not a direct call)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("g) After unregistering all observers, no MongoDB event is written")
    void g_unregisterAllObservers_noMongoEventWritten() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("bob@example.com");
        existingUser.setPreferences(new HashMap<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Remove the only registered observer from the chain
        userService.unregister(mongoEventLogger);

        // Act — same write that triggers USER_UPDATED in test f
        userService.mergePreferences(1L, Map.of("theme", "light"));

        // Assert: no save() call because no observer is left in the chain
        verify(authEventRepository, never()).save(any(AuthEvent.class));
    }
}
