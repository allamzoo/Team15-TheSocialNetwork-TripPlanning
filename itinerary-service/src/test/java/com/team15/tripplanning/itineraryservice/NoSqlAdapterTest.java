package com.team15.tripplanning.itineraryservice;

import com.team15.tripplanning.itineraryservice.adapter.MongoDocumentAdapter;
import com.team15.tripplanning.itineraryservice.adapter.Neo4jRecordAdapter;
import com.team15.tripplanning.itineraryservice.dto.ItineraryDetailsDTO;
import com.team15.tripplanning.itineraryservice.dto.TripCostEstimateDTO;
import com.team15.tripplanning.itineraryservice.model.mongo.ItineraryEvent;
import com.team15.tripplanning.itineraryservice.model.neo4j.TripRelationshipRecord;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoSqlAdapterTest {

    // ── a) both adapter classes exist ────────────────────────────────────
    @Test
    void a_mongoDocumentAdapterExists() throws Exception {
        assertNotNull(Class.forName(
                "com.team15.tripplanning.itineraryservice.adapter.MongoDocumentAdapter"));
    }

    @Test
    void a_neo4jRecordAdapterExists() throws Exception {
        assertNotNull(Class.forName(
                "com.team15.tripplanning.itineraryservice.adapter.Neo4jRecordAdapter"));
    }

    // ── b) return types match domain DTOs ────────────────────────────────
    @Test
    void b_mongoAdaptReturnsItineraryDetailsDTO() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.itineraryservice.adapter.MongoDocumentAdapter");
        Method adapt = cls.getMethod("adapt", ItineraryEvent.class);
        assertEquals(ItineraryDetailsDTO.class, adapt.getReturnType());
    }

    @Test
    void b_neo4jAdaptReturnsTripCostEstimateDTO() throws Exception {
        Class<?> cls = Class.forName(
                "com.team15.tripplanning.itineraryservice.adapter.Neo4jRecordAdapter");
        Method adapt = cls.getMethod("adapt", TripRelationshipRecord.class);
        assertEquals(TripCostEstimateDTO.class, adapt.getReturnType());
    }

    // ── c) MongoDocumentAdapter populates from ItineraryEvent ────────────
    @Test
    void c_mongoDocumentAdapterMapsItineraryEventToDto() {
        ItineraryEvent event = new ItineraryEvent(Map.of(
                "itineraryId",     10L,
                "userId",          3L,
                "action",          "ITINERARY_CREATED",
                "destinationId",   5L,
                "title",           "Greek Summer",
                "estimatedBudget", 2500.0
        ));

        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        ItineraryDetailsDTO dto = adapter.adapt(event);

        assertNotNull(dto);
        assertEquals(10L,                  dto.getItineraryId());
        assertEquals(3L,                   dto.getUserId());
        assertEquals("ITINERARY_CREATED",  dto.getStatus());
        assertEquals(5L,                   dto.getDestinationId());
        assertEquals("Greek Summer",       dto.getTitle());
        assertEquals(2500.0,               dto.getEstimatedBudget());
        assertNotNull(dto.getMetadata());
    }

    // ── c) MongoDocumentAdapter handles event with minimal fields ─────────
    @Test
    void c_mongoDocumentAdapterHandlesMinimalEvent() {
        ItineraryEvent event = new ItineraryEvent(Map.of(
                "itineraryId", 1L,
                "userId",      1L,
                "action",      "ITINERARY_DELETED"
        ));

        MongoDocumentAdapter adapter = new MongoDocumentAdapter();
        ItineraryDetailsDTO dto = adapter.adapt(event);

        assertNotNull(dto);
        assertEquals(1L, dto.getItineraryId());
        assertNull(dto.getDestinationId());
        assertNull(dto.getTitle());
        assertNull(dto.getEstimatedBudget());
    }

    // ── Neo4jRecordAdapter computes total = sum × multiplier ─────────────
    @Test
    void c_neo4jRecordAdapterComputesTotalCost() {
        TripRelationshipRecord record = new TripRelationshipRecord();
        record.setAccommodationCost(800.0);
        record.setTransportCost(300.0);
        record.setActivitiesCost(200.0);
        record.setSeasonMultiplier(1.2);

        Neo4jRecordAdapter adapter = new Neo4jRecordAdapter();
        TripCostEstimateDTO dto = adapter.adapt(record);

        assertEquals(800.0,   dto.getEstimatedAccommodation());
        assertEquals(300.0,   dto.getEstimatedTransport());
        assertEquals(200.0,   dto.getEstimatedActivities());
        assertEquals(1.2,     dto.getSeasonMultiplier());
        assertEquals(1560.0,  dto.getEstimatedTotal(), 0.001);
    }

    // ── Neo4jRecordAdapter defaults nulls to zero ─────────────────────────
    @Test
    void c_neo4jRecordAdapterDefaultsNullCostsToZero() {
        TripRelationshipRecord record = new TripRelationshipRecord();
        // all cost fields null, no multiplier

        Neo4jRecordAdapter adapter = new Neo4jRecordAdapter();
        TripCostEstimateDTO dto = adapter.adapt(record);

        assertEquals(0.0, dto.getEstimatedAccommodation());
        assertEquals(0.0, dto.getEstimatedTransport());
        assertEquals(0.0, dto.getEstimatedActivities());
        assertEquals(1.0, dto.getSeasonMultiplier());
        assertEquals(0.0, dto.getEstimatedTotal(), 0.001);
    }
}
