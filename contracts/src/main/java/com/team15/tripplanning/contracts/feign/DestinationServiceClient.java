package com.team15.tripplanning.contracts.feign;

import com.team15.tripplanning.contracts.dto.BatchDestinationRequest;
import com.team15.tripplanning.contracts.dto.DestinationDTO;
import com.team15.tripplanning.contracts.dto.DestinationSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Feign client for destination-service.
 * URL resolved from application.yml: feign.destination-service.url = http://destination-service:8080
 *
 * Used by: itinerary-service (S3-F2 assign, S3-F4 pre-check, S3-F11, S3-F12),
 *           booking-service (S5-F10 destination grouping).
 */
@FeignClient(name = "destination-service", url = "${feign.destination-service.url}")
public interface DestinationServiceClient {

    /**
     * Fetch destination by ID. Returns {id, name, country, category, status, rating, totalRatings, details}.
     *
     * Callers must wrap in try-catch:
     *  - FeignException.NotFound (404): destination does not exist → propagate 404
     *  - FeignException (other): destination-service unavailable
     *
     * S3-F2 additionally checks status == ACTIVE after the call.
     * S3-F4 additionally checks status == ACTIVE (destination pre-check before saga publish).
     */
    @GetMapping("/api/destinations/{id}")
    DestinationDTO getDestination(@PathVariable Long id);

    /**
     * S5-F10: batch lookup — {destinationIds:[…]} → [{destinationId, name, country, category}].
     * Folds N Feign calls into 1 for grouping booking revenue by destination name.
     */
    @PostMapping("/api/destinations/batch")
    List<DestinationSummaryDTO> batchGetDestinations(@RequestBody BatchDestinationRequest request);
}
