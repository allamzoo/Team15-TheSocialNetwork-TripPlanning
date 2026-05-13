package com.team15.tripplanning.contracts.feign.fallback;

import com.team15.tripplanning.contracts.dto.BatchDestinationRequest;
import com.team15.tripplanning.contracts.dto.DestinationDTO;
import com.team15.tripplanning.contracts.dto.DestinationSummaryDTO;
import com.team15.tripplanning.contracts.feign.DestinationServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Safe-default fallback for {@link DestinationServiceClient}.
 *
 * <p>{@link #getDestination(Long)} returns {@code null} — destination lookup is
 * a hard guard (S3-F2 must verify destination is ACTIVE before placing an
 * itinerary). Callers must null-check and return 503 when this happens.
 *
 * <p>{@link #batchGetDestinations(BatchDestinationRequest)} returns an empty list —
 * safe for revenue grouping (S5-F10 will produce empty groupings until
 * destination-service is deployed).
 */
public final class DestinationServiceFallback implements DestinationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DestinationServiceFallback.class);

    /** Singleton for use without Spring context. */
    public static final DestinationServiceFallback SAFE = new DestinationServiceFallback();

    /**
     * Returns {@code null} — callers must treat this as a hard failure (503).
     * Do NOT return a fake destination with status=ACTIVE; that would bypass
     * the S3-F2 active-destination guard.
     */
    @Override
    public DestinationDTO getDestination(Long id) {
        log.warn("destination-service fallback: getDestination({}) — returning null (hard guard)",
                id);
        return null;
    }

    @Override
    public List<DestinationSummaryDTO> batchGetDestinations(BatchDestinationRequest request) {
        log.debug("destination-service fallback: batchGetDestinations — returning empty list");
        return List.of();
    }
}
