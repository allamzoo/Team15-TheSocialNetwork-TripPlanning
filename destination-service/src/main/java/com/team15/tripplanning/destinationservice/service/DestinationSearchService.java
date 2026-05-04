package com.team15.tripplanning.destinationservice.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.UntypedRangeQuery;
import co.elastic.clients.json.JsonData;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationSearchDocument;
import com.team15.tripplanning.destinationservice.repository.DestinationSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DestinationSearchService {

    private static final Logger log = LoggerFactory.getLogger(DestinationSearchService.class);

    private final DestinationSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public DestinationSearchService(DestinationSearchRepository searchRepository,
                                    ElasticsearchOperations elasticsearchOperations) {
        this.searchRepository = searchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public List<DestinationSearchDocument> fullTextSearch(
            String query, String category, String status,
            Double minRating, Double maxRating) {

        BoolQuery.Builder bool = new BoolQuery.Builder();

        bool.must(Query.of(q -> q.multiMatch(MultiMatchQuery.of(mm -> mm
                .query(query)
                .fields("name", "description", "highlights")
                .fuzziness("AUTO")
        ))));

        if (category != null && !category.isBlank()) {
            bool.filter(Query.of(q -> q.term(TermQuery.of(t -> t
                    .field("category.keyword").value(category.toUpperCase())))));
        }

        if (status != null && !status.isBlank()) {
            bool.filter(Query.of(q -> q.term(TermQuery.of(t -> t
                    .field("status.keyword").value(status.toUpperCase())))));
        }

        if (minRating != null || maxRating != null) {
            Double finalMin = minRating;
            Double finalMax = maxRating;
            bool.filter(Query.of(q -> q.range(r -> r.untyped(UntypedRangeQuery.of(n -> {
                n.field("rating");
                if (finalMin != null) n.gte(JsonData.of(finalMin));
                if (finalMax != null) n.lte(JsonData.of(finalMax));
                return n;
            })))));
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(bool.build())))
                .build();

        SearchHits<DestinationSearchDocument> hits =
                elasticsearchOperations.search(nativeQuery, DestinationSearchDocument.class);

        List<DestinationSearchDocument> results = new ArrayList<>();
        for (SearchHit<DestinationSearchDocument> hit : hits) {
            results.add(hit.getContent());
        }
        return results;
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
            // ES is a soft dependency — indexing failure must never break the business operation
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("indexedFields", List.of());
            fallback.put("highlights", "");
            return fallback;
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

