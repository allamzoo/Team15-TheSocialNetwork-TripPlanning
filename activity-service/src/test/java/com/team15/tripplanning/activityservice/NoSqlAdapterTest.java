package com.team15.tripplanning.activityservice;

import com.team15.tripplanning.activityservice.adapter.CassandraRowAdapter;
import com.team15.tripplanning.activityservice.adapter.MongoDocumentAdapter;
import com.team15.tripplanning.activityservice.dto.ActivitySummaryDTO;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLogRow;
import com.team15.tripplanning.activityservice.model.mongo.ActivityEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoSqlAdapterTest {

    // ── a) both adapter classes exist ────────────────────────────────────
    @Test
    void a_mongoDocumentAdapterExists() throws Exception {
        assertNotNull(Class.forName(
                "com.team15.tripplanning.activityservice.adapter.MongoDocumentAdapter"));
    }

    @Test
    void a_cassandraRowAdapterExists() throws Exception {
        assertNotNull(Class.forName(
                "com.team15.tripplanning.activityservice.adapter.CassandraRowAdapter"));
    }

    // ── b) return types match domain DTOs ────────────────────────────────
    @Test
    void b_mongoAdaptReturnsActivitySummaryDTO() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.activityservice.adapter.MongoDocumentAdapter");
        Method adapt = cls.getMethod("adapt", ActivityEvent.class);
        assertEquals(ActivitySummaryDTO.class, adapt.getReturnType());
    }

    @Test
    void b_cassandraAdaptReturnsNearbyActivityDTO() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.activityservice.adapter.CassandraRowAdapter");
        Method adapt = cls.getMethod("adapt", ActivityLogRow.class);
        assertEquals(NearbyActivityDTO.class, adapt.getReturnType());
    }

    // ── c) MongoDocumentAdapter populates from ActivityEvent ─────────────
    @Test
    void c_mongoDocumentAdapterMapsActivityEventToDto() {
        ActivityEvent event = new ActivityEvent(Map.of(
                "activityId",  20L,
                "itineraryId", 5L,
                "action",      "ACTIVITY_CREATED",
                "cost",        150.0
        ));

        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        ActivitySummaryDTO dto = adapter.adapt(event);

        assertNotNull(dto);
        assertEquals(5L,    dto.getItineraryId());
        assertEquals(1,     dto.getTotalActivities());
        assertEquals(150.0, dto.getAverageCost());
        assertEquals(150.0, dto.getMaxCost());
        assertNotNull(dto.getFirstScheduledTime());
        assertNotNull(dto.getLastScheduledTime());
        assertEquals(dto.getFirstScheduledTime(), dto.getLastScheduledTime());
    }

    // ── c) MongoDocumentAdapter handles event with no cost field ─────────
    @Test
    void c_mongoDocumentAdapterHandlesMissingCost() {
        ActivityEvent event = new ActivityEvent(Map.of(
                "activityId",  1L,
                "itineraryId", 1L,
                "action",      "ACTIVITY_DELETED"
        ));

        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        ActivitySummaryDTO dto = adapter.adapt(event);

        assertNotNull(dto);
        assertNull(dto.getAverageCost());
        assertNull(dto.getMaxCost());
    }

    // ── CassandraRowAdapter maps log row to NearbyActivityDTO ────────────
    @Test
    void c_cassandraRowAdapterMapsRowToDto() {
        ActivityLogRow row = new ActivityLogRow();
        row.setActivityId(20L);
        row.setName("Acropolis Tour");
        row.setCategory("SIGHTSEEING");
        row.setLatitude(37.9715);
        row.setLongitude(23.7267);
        row.setCost(50.0);
        row.setItineraryId(5L);
        row.setScheduledTime(LocalDateTime.of(2025, 8, 10, 9, 0));

        CassandraRowAdapter adapter = new CassandraRowAdapter();
        NearbyActivityDTO dto = adapter.adapt(row);

        assertNotNull(dto);
        assertEquals(20L,             dto.getActivityId());
        assertEquals("Acropolis Tour", dto.getName());
        assertEquals("SIGHTSEEING",   dto.getCategory());
        assertEquals(37.9715,          dto.getLatitude());
        assertEquals(23.7267,          dto.getLongitude());
        assertEquals(0.0,              dto.getDistanceKm());
    }

    // ── CassandraRowAdapter handles null fields safely ────────────────────
    @Test
    void c_cassandraRowAdapterHandlesNullFields() {
        ActivityLogRow row = new ActivityLogRow();
        row.setActivityId(1L);

        CassandraRowAdapter adapter = new CassandraRowAdapter();
        NearbyActivityDTO dto = adapter.adapt(row);

        assertNotNull(dto);
        assertEquals(1L,  dto.getActivityId());
        assertNull(dto.getName());
        assertNull(dto.getLatitude());
        assertEquals(0.0, dto.getDistanceKm());
    }
}
