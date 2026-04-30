package com.team15.tripplanning.destinationservice;

import com.team15.tripplanning.destinationservice.adapter.ElasticsearchHitAdapter;
import com.team15.tripplanning.destinationservice.adapter.MongoDocumentAdapter;
import com.team15.tripplanning.destinationservice.dto.DestinationDTO;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.model.elasticsearch.DestinationSearchDocument;
import com.team15.tripplanning.destinationservice.model.mongo.DestinationEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoSqlAdapterTest {

    // ── a) both adapter classes exist ────────────────────────────────────
    @Test
    void a_mongoDocumentAdapterExists() throws Exception {
        assertNotNull(Class.forName(
                "com.team15.tripplanning.destinationservice.adapter.MongoDocumentAdapter"));
    }

    @Test
    void a_elasticsearchHitAdapterExists() throws Exception {
        assertNotNull(Class.forName(
                "com.team15.tripplanning.destinationservice.adapter.ElasticsearchHitAdapter"));
    }

    // ── b) both adapt() methods return DestinationDTO ────────────────────
    @Test
    void b_mongoAdaptReturnsDestinationDTO() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.destinationservice.adapter.MongoDocumentAdapter");
        Method adapt = cls.getMethod("adapt", DestinationEvent.class);
        assertEquals(DestinationDTO.class, adapt.getReturnType());
    }

    @Test
    void b_elasticsearchAdaptReturnsDestinationDTO() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.destinationservice.adapter.ElasticsearchHitAdapter");
        Method adapt = cls.getMethod("adapt", DestinationSearchDocument.class);
        assertEquals(DestinationDTO.class, adapt.getReturnType());
    }

    // ── c) MongoDocumentAdapter populates DTO from DestinationEvent ───────
    @Test
    void c_mongoDocumentAdapterMapsDestinationEventToDto() {
        DestinationEvent event = new DestinationEvent(Map.of(
                "destinationId", 7L,
                "action",        "DESTINATION_CREATED",
                "name",          "Santorini",
                "country",       "Greece",
                "category",      "BEACH",
                "rating",        4.8,
                "status",        "ACTIVE"
        ));

        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        DestinationDTO dto = adapter.adapt(event);

        assertNotNull(dto);
        assertEquals(7L,                        dto.getId());
        assertEquals("Santorini",               dto.getName());
        assertEquals("Greece",                  dto.getCountry());
        assertEquals(DestinationCategory.BEACH,  dto.getCategory());
        assertEquals(4.8,                        dto.getRating());
        assertEquals(DestinationStatus.ACTIVE,   dto.getStatus());
    }

    // ── d) ElasticsearchHitAdapter maps DestinationSearchDocument to DTO ──
    @Test
    void d_elasticsearchHitAdapterMapsDocumentToDto() {
        DestinationSearchDocument doc = new DestinationSearchDocument(
                "7", "Santorini", "Greece", "BEACH", 4.8, "ACTIVE"
        );

        ElasticsearchHitAdapter adapter = new ElasticsearchHitAdapter();
        DestinationDTO dto = adapter.adapt(doc);

        assertNotNull(dto);
        assertEquals(7L,                        dto.getId());
        assertEquals("Santorini",               dto.getName());
        assertEquals("Greece",                  dto.getCountry());
        assertEquals(DestinationCategory.BEACH,  dto.getCategory());
        assertEquals(4.8,                        dto.getRating());
        assertEquals(DestinationStatus.ACTIVE,   dto.getStatus());
    }

    // ── d) unknown category/status strings are handled safely (null) ──────
    @Test
    void d_elasticsearchAdapterHandlesUnknownEnumValuesSafely() {
        DestinationSearchDocument doc = new DestinationSearchDocument(
                "99", "Nowhere", "XX", "UNKNOWN_CAT", 0.0, "UNKNOWN_STATUS"
        );

        ElasticsearchHitAdapter adapter = new ElasticsearchHitAdapter();
        DestinationDTO dto = adapter.adapt(doc);

        assertNotNull(dto);
        assertEquals(99L, dto.getId());
        assertNull(dto.getCategory(), "Unknown category should map to null");
        assertNull(dto.getStatus(),   "Unknown status should map to null");
    }
}
