package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationSearchDocument;
import com.team15.tripplanning.destinationservice.repository.DestinationSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DestinationSearchService {

    private static final Logger log = LoggerFactory.getLogger(DestinationSearchService.class);

    private final DestinationSearchRepository searchRepository;

    public DestinationSearchService(DestinationSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Indexes a destination in Elasticsearch
     * Returns the list of indexed fields and the highlights text for event logging.
     *
     * @param destination the destination to index
     * @param source the source of the indexing ("explicit", "auto_crud_create", "auto_crud_update")
     * @return a map containing "indexedFields" list and "highlights" string
     */
    public Map<String, Object> indexDestination(Destination destination, String source) {
        try {
            // Extract highlights from details.topAttractions
            String highlights = extractHighlights(destination.getDetails());

            // Build the search document
            DestinationSearchDocument document = new DestinationSearchDocument(
                    destination.getId(),
                    destination.getName(),
                    destination.getCountry(),
                    destination.getCategory() != null ? destination.getCategory().name() : "",
                    destination.getDescription(),
                    highlights,
                    destination.getRating(),
                    destination.getTotalRatings(),
                    destination.getStatus() != null ? destination.getStatus().name() : "ACTIVE"
            );

            // Save to Elasticsearch
            searchRepository.save(document);

            // Prepare indexed fields list
            List<String> indexedFields = Arrays.asList(
                    "id", "name", "country", "category", "description",
                    "highlights", "rating", "totalRatings", "status"
            );

            Map<String, Object> result = new HashMap<>();
            result.put("indexedFields", indexedFields);
            result.put("highlights", highlights);

            log.info("Destination {} indexed successfully in Elasticsearch with source: {}", destination.getId(), source);
            return result;

        } catch (Exception e) {
            log.error("Failed to index destination {} in Elasticsearch: {}", destination.getId(), e.getMessage());
            throw new RuntimeException("Failed to index destination: " + e.getMessage(), e);
        }
    }

    /**
     * Removes a destination from Elasticsearch
     *
     * @param destinationId the ID of the destination to remove
     */
    public void removeDestination(Long destinationId) {
        try {
            searchRepository.deleteById(destinationId);
            log.info("Destination {} removed from Elasticsearch", destinationId);
        } catch (Exception e) {
            log.error("Failed to remove destination {} from Elasticsearch: {}", destinationId, e.getMessage());
            // Don't rethrow - it's a soft dependency
        }
    }

    /**
     * Extracts highlights from the topAttractions in details JSONB.
     * If topAttractions is missing or empty, returns an empty string.
     * Otherwise, joins the list into a whitespace-separated string.
     *
     * @param details the details map
     * @return the highlights string or empty string
     */
    private String extractHighlights(Map<String, Object> details) {
        if (details == null) {
            return "";
        }

        Object topAttractions = details.get("topAttractions");

        if (topAttractions == null) {
            return "";
        }

        if (!(topAttractions instanceof List<?>)) {
            return "";
        }

        List<?> list = (List<?>) topAttractions;
        if (list.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (item != null) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(item.toString());
            }
        }

        return sb.toString();
    }
}

