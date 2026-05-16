package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.dto.DestinationDashboardDTO;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.repository.DestinationDashboardRepository;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import feign.FeignException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.contracts.dto.DestinationDashboardAggregateDTO;

import java.util.Map;


@Service
public class DestinationDashboardService {

    private final DestinationRepository destinationRepository;
    private final DestinationDashboardRepository dashboardRepository;
    private final MongoEventLogger mongoEventLogger; // already exists in your project
    private final ItineraryServiceClient itineraryClient;


    public DestinationDashboardService(DestinationRepository destinationRepository,
                                       DestinationDashboardRepository dashboardRepository,
                                       MongoEventLogger mongoEventLogger,
                                       ItineraryServiceClient itineraryClient) {
        this.destinationRepository = destinationRepository;
        this.dashboardRepository = dashboardRepository;
        this.mongoEventLogger = mongoEventLogger;
        this.itineraryClient = itineraryClient;
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

    @Cacheable(value = "destination-service", key = "'S2-F12::' + #destinationId", unless = "#result == null")
    public DestinationDashboardDTO getCachedDashboard(Long destinationId, Destination destination) {
        DestinationDashboardAggregateDTO agg;
        try {
            agg = itineraryClient.getDestinationDashboardAggregate(destinationId);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dashboard data unavailable");
        }

        long totalItineraries = agg.totalItineraries();
        long completedItineraries = agg.completedItineraries();
        long totalVisitors = agg.totalVisitors();
        // The aggregate DTO does not directly include cancelled count or revenue;
        // if your existing dashboard DTO needs them, we can either add fields to the aggregate DTO
        // or keep them as 0 for now. Check the DestinationDashboardAggregateDTO definition.
        long cancelledItineraries = 0L; // if not in agg
        double totalRevenue = 0.0;      // if not in agg
        long totalBookings = 0L;        // if not in agg
        double completionRate = totalItineraries > 0
                ? (double) completedItineraries / totalItineraries * 100.0 : 0.0;

        return DestinationDashboardDTO.builder()
                .destinationId(destination.getId())
                .name(destination.getName())
                .totalItineraries(totalItineraries)
                .completedItineraries(completedItineraries)
                .cancelledItineraries(cancelledItineraries)
                .totalVisitors(totalVisitors)
                .totalRatings(destination.getTotalRatings())
                .averageRating(destination.getRating())
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .completionRate(completionRate)
                .build();
    }
}