package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.contracts.dto.DestinationDashboardAggregateDTO;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.destinationservice.dto.DestinationDashboardDTO;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class DestinationDashboardService {

    private static final Logger log = LoggerFactory.getLogger(DestinationDashboardService.class);

    private final DestinationRepository destinationRepository;
    private final ItineraryServiceClient itineraryServiceClient;
    private final MongoEventLogger mongoEventLogger;

    public DestinationDashboardService(DestinationRepository destinationRepository,
                                       ItineraryServiceClient itineraryServiceClient,
                                       MongoEventLogger mongoEventLogger) {
        this.destinationRepository = destinationRepository;
        this.itineraryServiceClient = itineraryServiceClient;
        this.mongoEventLogger = mongoEventLogger;
    }

    /**
     * MongoDB logging happens BEFORE cached method — fires on every request (hit or miss).
     */
    public DestinationDashboardDTO getDashboard(Long destinationId) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Destination not found with id: " + destinationId));

        mongoEventLogger.onEvent("DASHBOARD_VIEWED", Map.of(
                "destinationId", destinationId,
                "destinationName", destination.getName()
        ));

        return getCachedDashboard(destinationId, destination);
    }

    /**
     * Cached for 10 minutes. MongoDB logging is NOT here.
     * S2-F12: itinerary aggregate fetched via Feign — no cross-DB SQL.
     */
    @Cacheable(
            value = "destination-service",
            key = "'S2-F12::' + #destinationId",
            unless = "#result == null"
    )
    public DestinationDashboardDTO getCachedDashboard(Long destinationId, Destination destination) {
        long totalItineraries = 0L;
        long completedItineraries = 0L;
        long totalVisitors = 0L;

        MDC.put("destinationId", destinationId.toString());
        long start = System.currentTimeMillis();
        try {
            log.info("Calling itineraryServiceClient.getDestinationDashboardAggregate with args=[{}]", destinationId);
            DestinationDashboardAggregateDTO aggregate =
                    itineraryServiceClient.getDestinationDashboardAggregate(destinationId);
            log.info("itineraryServiceClient.getDestinationDashboardAggregate returned successfully");
            totalItineraries = aggregate.totalItineraries();
            completedItineraries = aggregate.completedItineraries();
            totalVisitors = aggregate.totalVisitors();
        } catch (FeignException e) {
            log.warn("Feign call to itinerary-service failed: {}", e.getMessage());
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > 1000) {
                log.warn("Slow getDestinationDashboardAggregate took {}ms", elapsed);
            }
            MDC.remove("destinationId");
        }

        double completionRate = totalItineraries > 0
                ? (double) completedItineraries / totalItineraries * 100.0 : 0.0;

        return DestinationDashboardDTO.builder()
                .destinationId(destination.getId())
                .name(destination.getName())
                .totalItineraries(totalItineraries)
                .completedItineraries(completedItineraries)
                .cancelledItineraries(0L)
                .totalVisitors(totalVisitors)
                .totalRatings(destination.getTotalRatings())
                .averageRating(destination.getRating())
                .totalRevenue(0.0)
                .totalBookings(0L)
                .completionRate(completionRate)
                .build();
    }
}
