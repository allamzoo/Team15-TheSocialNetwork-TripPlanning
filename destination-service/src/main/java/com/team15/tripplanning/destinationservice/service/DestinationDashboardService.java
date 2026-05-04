package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.dto.DestinationDashboardDTO;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.repository.DestinationDashboardRepository;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class DestinationDashboardService {

    private final DestinationRepository destinationRepository;
    private final DestinationDashboardRepository dashboardRepository;
    private final MongoEventLogger mongoEventLogger; // already exists in your project

    public DestinationDashboardService(DestinationRepository destinationRepository,
                                       DestinationDashboardRepository dashboardRepository,
                                       MongoEventLogger mongoEventLogger) {
        this.destinationRepository = destinationRepository;
        this.dashboardRepository = dashboardRepository;
        this.mongoEventLogger = mongoEventLogger;
    }

    /**
     * MongoDB logging happens BEFORE cached method — fires on every request (hit or miss)
     */
    public DestinationDashboardDTO getDashboard(Long destinationId) {
        // 1. Validate destination exists → 404 if not found
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found with id: " + destinationId));

        // 2. Log DASHBOARD_VIEWED on EVERY call (outside cache layer)
        mongoEventLogger.onEvent("DASHBOARD_VIEWED", Map.of(
                "destinationId", destinationId,
                "destinationName", destination.getName()
        ));

        // 3. Return cached result
        return getCachedDashboard(destinationId, destination);
    }

    /**
     * Cached for 10 minutes. MongoDB logging is NOT here.
     */
    @Cacheable(
            value = "destination-service",
            key = "'S2-F12::' + #destinationId",
            unless = "#result == null"
    )
    public DestinationDashboardDTO getCachedDashboard(Long destinationId, Destination destination) {
        // 4. Run aggregate query
        Object[] raw = dashboardRepository.getDashboardAggregates(destinationId);
        // Hibernate 6 may wrap the single row in an outer array
        Object[] agg = (raw.length > 0 && raw[0] instanceof Object[])
                ? (Object[]) raw[0]
                : raw;

        long totalItineraries     = agg[0] != null ? ((Number) agg[0]).longValue() : 0L;
        long completedItineraries = agg[1] != null ? ((Number) agg[1]).longValue() : 0L;
        long totalVisitors        = agg[2] != null ? ((Number) agg[2]).longValue() : 0L;

        // 5. Build DTO using Builder pattern
        return DestinationDashboardDTO.builder()
                .destinationId(destination.getId())
                .name(destination.getName())
                .totalItineraries(totalItineraries)
                .completedItineraries(completedItineraries)
                .totalVisitors(totalVisitors)
                .totalRatings(destination.getTotalRatings())
                .averageRating(destination.getRating())
                .build();
    }
}