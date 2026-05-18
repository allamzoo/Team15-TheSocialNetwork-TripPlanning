package com.team15.tripplanning.bookingservice.feature.aggregate;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.contracts.dto.BookingAggregateRequest;
import com.team15.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.team15.tripplanning.contracts.dto.ItineraryAggregateDTO;
import com.team15.tripplanning.contracts.dto.UserBookingTotalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class BookingAggregateService {

    private final BookingRepository bookingRepository;

    public BookingAggregateService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public UserBookingTotalDTO getUserTotal(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        Double total = bookingRepository.sumConfirmedAmountByUserAndRange(userId, start, end);
        long count = bookingRepository.countConfirmedByUserAndRange(userId, start, end);

        return new UserBookingTotalDTO(userId, BigDecimal.valueOf(total != null ? total : 0.0), count);
    }

    public ConfirmedSummaryDTO getConfirmedSummary(Long itineraryId) {
        // Use JPQL List<Object[]> variant — avoids Spring Boot 4.x native-query
        // Object[] wrapping where a single-row result comes back as Object[]{Object[]{col1,col2}}.
        List<Object[]> rows = bookingRepository.countAndSumConfirmed(itineraryId);
        long count = 0L;
        BigDecimal revenue = BigDecimal.ZERO;
        if (rows != null && !rows.isEmpty()) {
            Object[] row = rows.get(0);
            count = row[0] instanceof Number n ? n.longValue() : 0L;
            revenue = row[1] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
        }
        return new ConfirmedSummaryDTO(count, revenue);
    }

    public ItineraryAggregateDTO aggregateByItineraries(BookingAggregateRequest request) {
        if (request == null || request.itineraryIds() == null || request.itineraryIds().isEmpty()) {
            return new ItineraryAggregateDTO(0L, BigDecimal.ZERO);
        }

        LocalDate startDate = request.startDate() != null ? LocalDate.parse(request.startDate()) : LocalDate.now().minusYears(1);
        LocalDate endDate = request.endDate() != null ? LocalDate.parse(request.endDate()) : LocalDate.now();
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }

        Booking.BookingStatus status = null;
        if (request.status() != null && !request.status().isBlank()) {
            status = Booking.BookingStatus.valueOf(request.status().trim().toUpperCase());
        }

        Object[] raw = bookingRepository.aggregateByItineraryIds(
                request.itineraryIds(),
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59),
                status
        );

        // Defensive unwrap: Spring Boot 4.x / Hibernate 6 may return Object[]{Object[]{col1,col2}}
        // for a single-row scalar result — peel that extra wrapper if present.
        Object[] row = unwrapRow(raw);

        long count = 0L;
        BigDecimal total = BigDecimal.ZERO;
        if (row != null && row.length >= 2) {
            count = row[0] instanceof Number n ? n.longValue() : 0L;
            total = row[1] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
        }

        return new ItineraryAggregateDTO(count, total);
    }

    /**
     * Unwraps the extra Object[] layer that Spring Boot 4.x / Hibernate 6 sometimes
     * adds around a single-row native / JPQL scalar result.
     * Input: Object[]{Object[]{a, b}}  → returns Object[]{a, b}
     * Input: Object[]{a, b}            → returns as-is
     */
    private static Object[] unwrapRow(Object[] row) {
        if (row != null && row.length == 1 && row[0] instanceof Object[] inner) {
            return inner;
        }
        return row;
    }
}

