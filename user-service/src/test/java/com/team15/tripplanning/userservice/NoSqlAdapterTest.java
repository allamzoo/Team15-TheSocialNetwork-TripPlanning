package com.team15.tripplanning.userservice;

import com.team15.tripplanning.userservice.adapter.MongoDocumentAdapter;
import com.team15.tripplanning.userservice.adapter.ObjectArrayDtoAdapter;
import com.team15.tripplanning.userservice.document.AuthEvent;
import com.team15.tripplanning.userservice.dto.ActivityFeedDTO;
import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import com.team15.tripplanning.userservice.service.UserService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoSqlAdapterTest {

    // ── a) adapter class exists ───────────────────────────────────────────
    @Test
    void a_mongoDocumentAdapterClassExists() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.userservice.adapter.MongoDocumentAdapter");
        assertNotNull(cls);
    }

    // ── b) adapt() return type is ActivityFeedDTO ─────────────────────────
    @Test
    void b_adaptMethodReturnsActivityFeedDTO() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.userservice.adapter.MongoDocumentAdapter");
        Method adapt = cls.getMethod("adapt", AuthEvent.class);
        assertEquals(ActivityFeedDTO.class, adapt.getReturnType());
    }

    // ── c) MongoDocumentAdapter populates DTO from AuthEvent fields ────────
    @Test
    void c_mongoDocumentAdapterMapsAuthEventToDto() {
        AuthEvent event = new AuthEvent(Map.of(
                "userId", 42L,
                "action", "USER_CREATED"
        ));

        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        ActivityFeedDTO dto = adapter.adapt(event);

        assertNotNull(dto);
        assertEquals("USER_CREATED", dto.getAction());
        assertEquals(42L, dto.getUserId());
        assertNotNull(dto.getTimestamp());
        assertNotNull(dto.getDetails());
        assertEquals("USER_CREATED", dto.getDetails().get("action"));
    }

    // ── e) ObjectArrayDtoAdapter class exists via reflection ──────────────
    @Test
    void e_objectArrayDtoAdapterClassExists() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.userservice.adapter.ObjectArrayDtoAdapter");
        assertNotNull(cls);
        Method adapt = cls.getMethod("adapt", Object.class);
        assertEquals(UserTripSummaryDTO.class, adapt.getReturnType());
    }

    // ── e) ObjectArrayDtoAdapter is wired inside UserService ─────────────
    @Test
    void e_objectArrayDtoAdapterIsUsedInUserService() {
        boolean found = Arrays.stream(UserService.class.getDeclaredFields())
                .anyMatch(f -> f.getType().equals(ObjectArrayDtoAdapter.class));
        assertTrue(found, "UserService must declare an ObjectArrayDtoAdapter field");
    }

    // ── e) ObjectArrayDtoAdapter converts Object[] row to correct DTO ─────
    @Test
    void e_objectArrayDtoAdapterConvertsRowToDto() {
        // columns: userId, name, totalTrips, completedTrips, cancelledTrips, totalSpent, averageBudget
        Object[] row = new Object[]{1L, "Alice", 5L, 3L, 1L, 1200.0, 240.0};

        ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
        UserTripSummaryDTO dto = adapter.adapt(row);

        assertEquals(1L,      dto.getUserId());
        assertEquals("Alice", dto.getName());
        assertEquals(5L,      dto.getTotalTrips());
        assertEquals(3L,      dto.getCompletedTrips());
        assertEquals(1L,      dto.getCancelledTrips());
        assertEquals(1200.0,  dto.getTotalSpent());
        assertEquals(240.0,   dto.getAverageBudget());
    }

    // ── f) no other M1 features use raw Object[] — ObjectArrayDtoAdapter
    //       is the sole adapter for Object[] conversion (S1-F3 only) ───────
    @Test
    void f_onlyObjectArrayDtoAdapterHandlesObjectArrayRows() throws Exception {
        Class<?> adapterClass = Class.forName(
                "com.team15.tripplanning.userservice.adapter.ObjectArrayDtoAdapter");
        // Verify UserService references it (not raw Object[] casts inline)
        Field[] fields = UserService.class.getDeclaredFields();
        long adapterCount = Arrays.stream(fields)
                .filter(f -> f.getType().equals(adapterClass))
                .count();
        assertEquals(1, adapterCount,
                "Exactly one ObjectArrayDtoAdapter field expected in UserService");
    }
}
