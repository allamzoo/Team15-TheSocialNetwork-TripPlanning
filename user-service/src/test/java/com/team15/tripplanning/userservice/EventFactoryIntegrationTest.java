package com.team15.tripplanning.userservice;

import com.team15.tripplanning.userservice.controller.AuthController;
import com.team15.tripplanning.userservice.document.AuthEvent;
import com.team15.tripplanning.userservice.model.Role;
import com.team15.tripplanning.userservice.model.Status;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.observer.EventFactory;
import com.team15.tripplanning.userservice.observer.EventType;
import com.team15.tripplanning.userservice.observer.MongoEvent;
import com.team15.tripplanning.userservice.observer.MongoEventLogger;
import com.team15.tripplanning.userservice.messaging.publisher.UserEventPublisher;
import com.team15.tripplanning.userservice.repository.AuthEventRepository;
import com.team15.tripplanning.userservice.repository.UserRepository;
import com.team15.tripplanning.userservice.security.JwtService;
import com.team15.tripplanning.userservice.service.AuthService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Factory pattern — scenario g.
 *
 * Calls POST /api/auth/register via a real HTTP stack (MockMvc standaloneSetup,
 * no Spring context needed), then asserts that the document saved to auth_events
 * has the same structure that EventFactory.createEvent(AUTH, ...) would produce.
 */
class EventFactoryIntegrationTest {

    private MockMvc mockMvc;
    private AuthEventRepository authEventRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        authEventRepository = mock(AuthEventRepository.class);
        userRepository      = mock(UserRepository.class);

        // Real collaborators — no Spring context needed
        JwtService        jwtService        = new JwtService();
        MongoEventLogger  mongoEventLogger  = new MongoEventLogger(authEventRepository);
        AuthService       authService       = new AuthService(
                userRepository,
                new BCryptPasswordEncoder(),
                jwtService,
                mongoEventLogger,
                mock(UserEventPublisher.class)
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .build();
    }

    // ── g ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("g) POST /api/auth/register saves an AuthEvent whose structure matches EventFactory.createEvent(AUTH,...)")
    void g_register_savesAuthEventMatchingFactoryStructure() throws Exception {
        // ── Arrange ──────────────────────────────────────────────────────────
        User savedUser = new User();
        savedUser.setId(55L);
        savedUser.setEmail("factory@test.com");
        savedUser.setRole(Role.TRAVELER);
        savedUser.setStatus(Status.ACTIVE);

        when(userRepository.findByEmail("factory@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+20100000001")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // ── Act — real HTTP call ──────────────────────────────────────────────
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":     "Factory Test",
                                  "email":    "factory@test.com",
                                  "password": "Test@1234",
                                  "phone":    "+20100000001"
                                }
                                """))
                .andExpect(status().isCreated());

        // ── Assert: what was saved to MongoDB ────────────────────────────────
        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository, times(1)).save(captor.capture());

        AuthEvent saved = captor.getValue();

        // 1. Action must be REGISTERED
        assertEquals("REGISTERED", saved.getAction(),
                "action must be REGISTERED");

        // 2. userId must match the persisted user
        assertEquals(55L, saved.getUserId(),
                "userId must match the registered user's id");

        // 3. Timestamp must be set (factory always sets it)
        assertNotNull(saved.getTimestamp(),
                "timestamp must not be null — factory sets it at creation time");

        // 4. Structure must match what EventFactory.createEvent(AUTH, params) produces
        Map<String, Object> factoryParams = new HashMap<>();
        factoryParams.put("action", "REGISTERED");
        factoryParams.put("userId", 55L);
        MongoEvent factoryEvent = EventFactory.createEvent(EventType.AUTH, factoryParams);

        assertEquals(factoryEvent.getAction(), saved.getAction(),
                "action must equal EventFactory output");
        assertNotNull(factoryEvent.getTimestamp(),
                "factory event must also have a non-null timestamp");

        // 5. Only one REGISTERED event must be saved — not two, not zero
        verify(authEventRepository, times(1)).save(any(AuthEvent.class));
    }
}
