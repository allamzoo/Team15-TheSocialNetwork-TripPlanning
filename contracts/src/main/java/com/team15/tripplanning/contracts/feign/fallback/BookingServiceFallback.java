package com.team15.tripplanning.contracts.feign.fallback;

import com.team15.tripplanning.contracts.dto.BookingAggregateRequest;
import com.team15.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.team15.tripplanning.contracts.dto.ItineraryAggregateDTO;
import com.team15.tripplanning.contracts.dto.UserBookingTotalDTO;
import com.team15.tripplanning.contracts.feign.BookingServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Safe-default fallback for {@link BookingServiceClient}.
 *
 * <p>Critical note on {@link #getConfirmedSummary(Long)}: the fallback returns
 * {@code count=0}, which causes the S3 saga pre-check ({@code count >= 1}) to
 * <b>fail safely</b> — the saga will not trigger if booking-service is unreachable.
 * This is intentional: it is safer to block a completion than to trigger an
 * accidental payment saga.
 */
public final class BookingServiceFallback implements BookingServiceClient {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceFallback.class);

    /** Singleton for use without Spring context. */
    public static final BookingServiceFallback SAFE = new BookingServiceFallback();

    @Override
    public UserBookingTotalDTO getUserBookingTotal(Long userId, String startDate, String endDate) {
        log.debug("booking-service fallback: getUserBookingTotal({})", userId);
        return new UserBookingTotalDTO(userId, BigDecimal.ZERO, 0L);
    }

    @Override
    public ItineraryAggregateDTO aggregateByItineraries(BookingAggregateRequest request) {
        log.debug("booking-service fallback: aggregateByItineraries");
        return new ItineraryAggregateDTO(0L, BigDecimal.ZERO);
    }

    /**
     * Returns {@code count=0} — the S3 saga pre-check will fail safely
     * (won't proceed with itinerary completion when booking-service is down).
     */
    @Override
    public ConfirmedSummaryDTO getConfirmedSummary(Long itineraryId) {
        log.warn("booking-service fallback: getConfirmedSummary({}) — count=0, saga blocked",
                itineraryId);
        return new ConfirmedSummaryDTO(0L, BigDecimal.ZERO);
    }
}
