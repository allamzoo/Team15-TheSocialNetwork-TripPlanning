package com.team15.tripplanning.destinationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.repository.DestinationReviewRepository;
import com.team15.tripplanning.destinationservice.service.DestinationSearchService;
import com.team15.tripplanning.destinationservice.service.DestinationService;
import com.team15.tripplanning.destinationservice.service.MongoEventLogger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DestinationIndexingServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private DestinationReviewRepository destinationReviewRepository;

    @Mock
    private MongoEventLogger mongoEventLogger;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DestinationSearchService searchService;

    @InjectMocks
    private DestinationService destinationService;

    @Test
    void createAutoIndexesAndLogsIndexedEvent() {
        Destination incoming = buildDestination("Santorini", "Greece", "idyllic Greek island with caldera views");
        incoming.setDetails(new HashMap<>(Map.of(
                "topAttractions", List.of("Oia sunset", "Red Beach")
        )));

        Destination saved = buildDestination("Santorini", "Greece", "idyllic Greek island with caldera views");
        saved.setId(10L);
        saved.setDetails(incoming.getDetails());

        when(destinationRepository.save(any(Destination.class))).thenReturn(saved);
        when(searchService.indexDestination(eq(saved), eq("auto_crud_create"))).thenReturn(Map.of(
                "indexedFields", List.of("id", "name", "country", "category", "description", "highlights", "rating", "totalRatings", "status"),
                "highlights", "Oia sunset Red Beach"
        ));

        Destination result = destinationService.create(incoming);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(searchService).indexDestination(eq(saved), eq("auto_crud_create"));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mongoEventLogger).onEvent(eq("INDEXED"), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals(10L, payload.get("destinationId"));
        assertEquals("Santorini", payload.get("name"));
        assertEquals("auto_crud_create", payload.get("source"));
        assertEquals(List.of("id", "name", "country", "category", "description", "highlights", "rating", "totalRatings", "status"), payload.get("indexedFields"));
    }

    @Test
    void createStillSucceedsWhenElasticsearchIndexingFails() {
        Destination incoming = buildDestination("Santorini", "Greece", "idyllic Greek island with caldera views");
        incoming.setDetails(new HashMap<>(Map.of(
                "topAttractions", List.of("Oia sunset", "Red Beach")
        )));

        Destination saved = buildDestination("Santorini", "Greece", "idyllic Greek island with caldera views");
        saved.setId(11L);
        saved.setDetails(incoming.getDetails());

        when(destinationRepository.save(any(Destination.class))).thenReturn(saved);
        when(searchService.indexDestination(eq(saved), eq("auto_crud_create")))
                .thenThrow(new RuntimeException("media_type_header_exception"));
        doThrow(new RuntimeException("observer failure")).when(mongoEventLogger).onEvent(eq("INDEXED"), any());
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("redis down"));

        Destination result = destinationService.create(incoming);

        assertNotNull(result);
        assertEquals(11L, result.getId());
        verify(searchService).indexDestination(eq(saved), eq("auto_crud_create"));
        verify(mongoEventLogger).onEvent(eq("INDEXED"), any());
    }

    @Test
    void updateAutoIndexesAndLogsIndexedEvent() {
        Destination existing = buildDestination("Mykonos", "Greece", "beach club destination");
        existing.setId(20L);
        existing.setDetails(new HashMap<>());

        Destination incoming = buildDestination("Mykonos Town", "Greece", "beach club destination");
        incoming.setCategory(DestinationCategory.BEACH);
        incoming.setStatus(DestinationStatus.ACTIVE);

        when(destinationRepository.findById(20L)).thenReturn(java.util.Optional.of(existing));
        when(destinationRepository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(searchService.indexDestination(any(Destination.class), eq("auto_crud_update"))).thenReturn(Map.of(
                "indexedFields", List.of("id", "name"),
                "highlights", ""
        ));

        Destination result = destinationService.update(20L, incoming);

        assertNotNull(result);
        assertEquals("Mykonos Town", result.getName());
        verify(searchService).indexDestination(any(Destination.class), eq("auto_crud_update"));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mongoEventLogger).onEvent(eq("INDEXED"), payloadCaptor.capture());
        assertEquals(20L, payloadCaptor.getValue().get("destinationId"));
        assertEquals("auto_crud_update", payloadCaptor.getValue().get("source"));
    }

    @Test
    void deleteRemovesSearchDocumentAndLogsDeletedEvent() {
        Destination existing = buildDestination("Naxos", "Greece", "ancient island");
        existing.setId(30L);

        when(destinationRepository.findById(30L)).thenReturn(java.util.Optional.of(existing));

        destinationService.delete(30L);

        verify(destinationRepository).delete(existing);
        verify(searchService).removeDestination(30L);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mongoEventLogger).onEvent(eq("DESTINATION_DELETED"), payloadCaptor.capture());
        assertEquals(30L, payloadCaptor.getValue().get("destinationId"));
        assertEquals("auto_crud_delete", payloadCaptor.getValue().get("source"));
    }

    @Test
    void explicitIndexUsesExplicitSourceAndMissingDestinationReturns404() {
        Destination existing = buildDestination("Corfu", "Greece", "green island");
        existing.setId(40L);

        when(destinationRepository.findById(40L)).thenReturn(java.util.Optional.of(existing));
        when(searchService.indexDestination(existing, "explicit")).thenReturn(Map.of(
                "indexedFields", List.of("id", "name")
        ));

        destinationService.indexDestinationExplicit(40L);

        verify(searchService).indexDestination(existing, "explicit");
        verify(mongoEventLogger).onEvent(eq("INDEXED"), any());

        when(destinationRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> destinationService.indexDestinationExplicit(999L));
        assertEquals(404, ex.getStatusCode().value());
    }

    private static Destination buildDestination(String name, String country, String description) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setCountry(country);
        destination.setDescription(description);
        destination.setCategory(DestinationCategory.BEACH);
        destination.setStatus(DestinationStatus.ACTIVE);
        destination.setRating(4.5);
        destination.setTotalRatings(12);
        destination.setDetails(new HashMap<>());
        return destination;
    }
}


