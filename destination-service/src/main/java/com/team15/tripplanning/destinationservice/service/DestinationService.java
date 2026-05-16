package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.contracts.dto.ItineraryDTO;
import com.team15.tripplanning.contracts.dto.UserDTO;

import com.team15.tripplanning.contracts.feign.UserServiceClient;
import com.team15.tripplanning.destinationservice.dto.DestinationRateRequest;
import com.team15.tripplanning.destinationservice.dto.DestinationRevenueDTO;
import com.team15.tripplanning.destinationservice.dto.DestinationReviewAlertDTO;
import com.team15.tripplanning.destinationservice.dto.TopDestinationDTO;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.repository.DestinationReviewRepository;
import com.team15.tripplanning.shared.observer.EntityObserver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.dto.DestinationBookingRevenueAggregateDTO;

import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.destinationservice.messaging.publisher.DestinationEventPublisher;

@Service
public class DestinationService {
    private static final Logger log = LoggerFactory.getLogger(DestinationService.class);
    private final DestinationRepository destinationRepository;
    private final DestinationReviewRepository destinationReviewRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final DestinationSearchService searchService;
    private final ItineraryServiceClient itineraryClient;
    private final DestinationEventPublisher eventPublisher;
    private final UserServiceClient userClient;


    public DestinationService(DestinationRepository destinationRepository,
                              DestinationReviewRepository destinationReviewRepository,
                              MongoEventLogger mongoEventLogger,
                              RedisTemplate<String, Object> redisTemplate,
                              DestinationSearchService searchService,
                              ItineraryServiceClient itineraryClient,
                              DestinationEventPublisher eventPublisher,
                              UserServiceClient userClient) {
        this.destinationRepository = destinationRepository;
        this.destinationReviewRepository = destinationReviewRepository;
        this.redisTemplate = redisTemplate;
        this.searchService = searchService;
        this.itineraryClient = itineraryClient;
        this.eventPublisher = eventPublisher;
        this.userClient = userClient;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    private void deleteWildcard(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // Redis is a soft dependency — cache eviction failure must never break the business operation
            log.warn("Cache eviction skipped (Redis unavailable) for pattern {}: {}", pattern, e.getMessage());
        }
    }

    public Destination create(Destination destination) {
        Destination saved = destinationRepository.save(destination);
        
        // Auto-index in Elasticsearch
        Map<String, Object> indexResult = searchService.indexDestination(saved, "auto_crud_create");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("destinationId", saved.getId());
        payload.put("name", saved.getName());
        payload.put("indexedFields", indexResult.get("indexedFields"));
        payload.put("source", "auto_crud_create");
        notifyObservers("INDEXED", payload);
        
        deleteWildcard("s2-destinations::*");
        deleteWildcard("s2-top-rated::*");
        deleteWildcard("s2-dest-search::*");
        return saved;
    }

    public List<Destination> findAll() {
        return destinationRepository.findAll();
    }

    public Destination findById(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found: " + id));
    }

    public Destination update(Long id, Destination destination) {
        Destination existing = findById(id);
        existing.setName(destination.getName());
        existing.setCountry(destination.getCountry());
        existing.setDescription(destination.getDescription());
        existing.setCategory(destination.getCategory());
        existing.setStatus(destination.getStatus());
        existing.setRating(destination.getRating());
        existing.setTotalRatings(destination.getTotalRatings());
        existing.setDetails(destination.getDetails());
        Destination saved = destinationRepository.save(existing);
        
        // Auto-index in Elasticsearch
        Map<String, Object> indexResult = searchService.indexDestination(saved, "auto_crud_update");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("destinationId", saved.getId());
        payload.put("name", saved.getName());
        payload.put("indexedFields", indexResult.get("indexedFields"));
        payload.put("source", "auto_crud_update");
        notifyObservers("INDEXED", payload);
        
        deleteWildcard("s2-destinations::*");
        deleteWildcard("s2-top-rated::*");
        deleteWildcard("s2-dest-search::*");
        deleteWildcard("s2-dest-revenue::S2::S2-F*::" + id + "*");
        return saved;
    }

    public void delete(Long id) {
        Destination destination = findById(id);
        destinationRepository.delete(destination);
        
        // Remove from Elasticsearch
        searchService.removeDestination(id);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("destinationId", id);
        payload.put("source", "auto_crud_delete");
        notifyObservers("DESTINATION_DELETED", payload);
        
        deleteWildcard("s2-destinations::*");
        deleteWildcard("s2-top-rated::*");
        deleteWildcard("s2-dest-search::*");
        deleteWildcard("s2-dest-revenue::S2::S2-F*::" + id + "*");
    }

    @Cacheable(value = "s2-dest-search", key = "'S2::S2-F1::' + #category + '::' + #minRating + '::' + #maxRating")
    public List<Destination> searchDestinations(DestinationCategory category, Double minRating, Double maxRating) {
        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating cannot be greater than maxRating");
        }

        double min = (minRating != null) ? minRating : 0.0;
        double max = (maxRating != null) ? maxRating : 5.0;

        return destinationRepository.searchDestinations(category, min, max);
    }

    public Destination updateDestinationDetails(Long id, Map<String, Object> incomingDetails) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found: " + id));

        Map<String, Object> existingDetails = destination.getDetails();
        if (existingDetails == null) {
            existingDetails = new HashMap<>();
        }

        if (incomingDetails != null) {
            existingDetails.putAll(incomingDetails);
        }

        destination.setDetails(existingDetails);
        Destination saved = destinationRepository.save(destination);
        Map<String, Object> payload = new HashMap<>();
        payload.put("destinationId", saved.getId());
        notifyObservers("DETAILS_UPDATED", payload);
        deleteWildcard("s2-destinations::*");
        deleteWildcard("s2-dest-search::*");
        return saved;
    }

    @Transactional
    public Destination updateDestinationStatus(Long id, String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        DestinationStatus newStatus;
        try {
            newStatus = DestinationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be one of ACTIVE, SEASONAL, INACTIVE");
        }

        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Destination not found: " + id));

        String oldStatus = destination.getStatus().name();   // capture for the event

        // When deactivating, check active itineraries via Feign
        if (newStatus == DestinationStatus.INACTIVE) {
            int activeCount;
            try {
                activeCount = itineraryClient.getDestinationActiveCount(id);
            } catch (FeignException e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Unable to verify active itineraries at this time");
            }
            if (activeCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot set destination to INACTIVE: " + activeCount + " active itinerary(ies) reference it");
            }
        }

        destination.setStatus(newStatus);
        Destination saved = destinationRepository.save(destination);

        // Publish event
        eventPublisher.publishStatusChanged(saved.getId(), oldStatus, newStatus.name());

        // Invalidate caches
        deleteWildcard("s2-destinations::*");
        deleteWildcard("s2-top-rated::*");
        deleteWildcard("s2-dest-search::*");
        deleteWildcard("destination-service::S2-F3::" + id + "::*");
        deleteWildcard("destination-service::S2-F12::" + id);

        Map<String, Object> payload = new HashMap<>();
        payload.put("destinationId", saved.getId());
        payload.put("status", newStatus.name());
        notifyObservers("STATUS_CHANGED", payload);
        return saved;
    }

    @Cacheable(value = "destination-service", key = "'S2-F3::' + #id + '::' + #startDate + '::' + #endDate")
    public DestinationRevenueDTO getDestinationRevenueSummary(Long id, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found: " + id));

        // NEW: call itinerary-service via Feign
        DestinationBookingRevenueAggregateDTO agg;
        try {
            agg = itineraryClient.getDestinationBookingRevenue(id, startDate.toString(), endDate.toString());
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Revenue data temporarily unavailable");
        }

        return DestinationRevenueDTO.builder()
                .destinationId(destination.getId())
                .name(destination.getName())
                .totalBookings(agg.totalBookings())
                .totalRevenue(agg.totalRevenue() != null ? agg.totalRevenue().doubleValue() : 0.0)
                .averageBookingAmount(agg.averageBookingAmount() != null ? agg.averageBookingAmount().doubleValue() : 0.0)
                .build();
    }

    public List<Destination> filterByDetailAttribute(String key, String value, String status) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key and value are required");
        }

        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                normalizedStatus = DestinationStatus.valueOf(status.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid status. Must be one of ACTIVE, SEASONAL, INACTIVE");
            }
        }

        return destinationRepository.findByDetailAttribute(key, value, normalizedStatus);
    }

    @Cacheable(value = "s2-top-rated", key = "'S2::S2-F3::' + #limit")
    public List<TopDestinationDTO> getTopRatedDestinations(int limit) {
        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than 0");
        }

        List<Object[]> rows = destinationRepository.findTopRatedWithBookingCount(limit);

        return rows.stream().map(row -> TopDestinationDTO.builder()
                .destinationId(((Number) row[0]).longValue())
                .name((String) row[1])
                .rating(row[2] == null ? 0.0 : ((Number) row[2]).doubleValue())
                .totalBookings(row[3] == null ? 0L : ((Number) row[3]).longValue())
                .build()).toList();
    }

    @Transactional
    public Destination rateDestination(Long destinationId, DestinationRateRequest request) {
        if (request.rating() < 1 || request.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        // Validate itinerary via Feign
        ItineraryDTO itinerary;
        try {
            itinerary = itineraryClient.getItinerary(request.itineraryId());
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Itinerary service unavailable");
        }

        // Check destination match
        if (!itinerary.destinationId().equals(destinationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary does not belong to this destination");
        }

        // Check status is COMPLETED or PAID
        if (!List.of("COMPLETED", "PAID").contains(itinerary.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary is not completed or paid");
        }

        // Recompute rating
        double currentAvg = (destination.getRating() != null) ? destination.getRating() : 0.0;
        int currentTotal = (destination.getTotalRatings() != null) ? destination.getTotalRatings() : 0;
        double newAvg = ((currentAvg * currentTotal) + request.rating()) / (currentTotal + 1);
        destination.setRating(newAvg);
        destination.setTotalRatings(currentTotal + 1);
        Destination saved = destinationRepository.save(destination);

        // Publish event
        eventPublisher.publishRated(saved.getId(), request.itineraryId(), Double.valueOf(request.rating()), itinerary.userId());

        // Invalidate caches
        deleteWildcard("s2-destinations::*");
        deleteWildcard("s2-top-rated::*");
        deleteWildcard("s2-dest-search::*");

        Map<String, Object> payload = new HashMap<>();
        payload.put("destinationId", saved.getId());
        payload.put("rating", request.rating());
        notifyObservers("RATING_ADDED", payload);
        return saved;
    }

    @Transactional
    public Destination verifyReview(Long destinationId, Long reviewId, Long verifierId) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        DestinationReview review = destinationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        if (!review.getDestination().getId().equals(destinationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review does not belong to this destination");
        }

        if (review.getVisitDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot verify a review for a future visit");
        }

        // Feign call to user-service for ADMIN check
        UserDTO verifier;
        try {
            verifier = userClient.getUser(verifierId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User service unavailable");
        }
        if (!"ADMIN".equals(verifier.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not an Admin");
        }

        // Mark as verified
        review.setVerified(true);
        Map<String, Object> metadata = review.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("verifiedAt", LocalDateTime.now().toString());
        metadata.put("verifiedBy", verifierId);
        review.setMetadata(metadata);
        destinationReviewRepository.save(review);

        Map<String, Object> payload = new HashMap<>();
        payload.put("destinationId", destinationId);
        payload.put("reviewId", reviewId);
        payload.put("verifiedBy", verifierId);
        notifyObservers("REVIEW_VERIFIED", payload);

        destination.getDestinationReviews().size();
        return destination;
    }

    @Transactional
    public List<DestinationReviewAlertDTO> getLowRatedReviewAlerts(Double maxRating) {
        return destinationRepository.findAll().stream()
                .map(dest -> {
                    List<DestinationReview> lowReviews = dest.getDestinationReviews().stream()
                            .filter(r -> r.getRating() <= maxRating)
                            .toList();

                    if (!lowReviews.isEmpty()) {
                        return DestinationReviewAlertDTO.builder()
                                .destinationId(dest.getId())
                                .destinationName(dest.getName())
                                .destinationStatus(dest.getStatus())
                                .lowRatedReviews(lowReviews)
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Explicitly index a destination (invoked via POST /api/destinations/{id}/index)
     * Finds the destination by ID, indexes it in Elasticsearch, and logs an INDEXED event with source="explicit"
     *
     * @param id the destination ID
     * @throws ResponseStatusException if destination not found (404) or indexing fails
     */
    public void indexDestinationExplicit(Long id) {
        Destination destination = findById(id);
        
        // Index in Elasticsearch with source="explicit"
        Map<String, Object> indexResult = searchService.indexDestination(destination, "explicit");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("destinationId", destination.getId());
        payload.put("indexedFields", indexResult.get("indexedFields"));
        payload.put("source", "explicit");
        notifyObservers("INDEXED", payload);
    }
}
