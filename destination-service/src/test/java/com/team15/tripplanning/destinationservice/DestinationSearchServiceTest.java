package com.team15.tripplanning.destinationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationSearchDocument;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.repository.DestinationSearchRepository;
import com.team15.tripplanning.destinationservice.service.DestinationSearchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DestinationSearchServiceTest {

    @Mock
    private DestinationSearchRepository searchRepository;

    @InjectMocks
    private DestinationSearchService searchService;

    @Test
    void indexDestinationFlattensTopAttractionsIntoHighlights() {
        Destination destination = buildDestination();
        destination.setDetails(new HashMap<>(Map.of(
                "topAttractions", List.of("Oia sunset", "Red Beach")
        )));

        when(searchRepository.save(any(DestinationSearchDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = searchService.indexDestination(destination, "explicit");

        ArgumentCaptor<DestinationSearchDocument> captor = ArgumentCaptor.forClass(DestinationSearchDocument.class);
        verify(searchRepository).save(captor.capture());

        DestinationSearchDocument doc = captor.getValue();
        assertNotNull(doc);
        assertEquals(88L, doc.getId());
        assertEquals("Santorini", doc.getName());
        assertEquals("Greece", doc.getCountry());
        assertEquals("BEACH", doc.getCategory());
        assertEquals("idyllic Greek island with caldera views", doc.getDescription());
        assertEquals("Oia sunset Red Beach", doc.getHighlights());
        assertEquals(4.8, doc.getRating());
        assertEquals(42, doc.getTotalRatings());
        assertEquals("ACTIVE", doc.getStatus());

        assertEquals("Oia sunset Red Beach", result.get("highlights"));
        assertEquals(List.of("id", "name", "country", "category", "description", "highlights", "rating", "totalRatings", "status"), result.get("indexedFields"));
    }

    @Test
    void indexDestinationUsesEmptyHighlightsWhenMissingTopAttractions() {
        Destination destination = buildDestination();
        destination.setDetails(Map.of());

        when(searchRepository.save(any(DestinationSearchDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = searchService.indexDestination(destination, "auto_crud_create");

        ArgumentCaptor<DestinationSearchDocument> captor = ArgumentCaptor.forClass(DestinationSearchDocument.class);
        verify(searchRepository).save(captor.capture());

        assertEquals("", captor.getValue().getHighlights());
        assertEquals("", result.get("highlights"));
    }

    @Test
    void indexDestinationUsesEmptyHighlightsWhenTopAttractionsListIsEmpty() {
        Destination destination = buildDestination();
        destination.setDetails(new HashMap<>(Map.of("topAttractions", List.of())));

        when(searchRepository.save(any(DestinationSearchDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = searchService.indexDestination(destination, "auto_crud_update");

        ArgumentCaptor<DestinationSearchDocument> captor = ArgumentCaptor.forClass(DestinationSearchDocument.class);
        verify(searchRepository).save(captor.capture());

        assertEquals("", captor.getValue().getHighlights());
        assertEquals("", result.get("highlights"));
    }

    private static Destination buildDestination() {
        Destination destination = new Destination();
        destination.setId(88L);
        destination.setName("Santorini");
        destination.setCountry("Greece");
        destination.setDescription("idyllic Greek island with caldera views");
        destination.setCategory(DestinationCategory.BEACH);
        destination.setStatus(DestinationStatus.ACTIVE);
        destination.setRating(4.8);
        destination.setTotalRatings(42);
        return destination;
    }
}
