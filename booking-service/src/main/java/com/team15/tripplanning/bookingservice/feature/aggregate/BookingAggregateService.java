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
        Object[] row = bookingRepository.getConfirmedSummaryByItinerary(itineraryId);
        long count = 0L;
        BigDecimal revenue = BigDecimal.ZERO;
        if (row != null && row.length >= 2) {
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

        Object[] row = bookingRepository.aggregateByItineraryIds(
                request.itineraryIds(),
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59),
                status
        );

        long count = 0L;
        BigDecimal total = BigDecimal.ZERO;
        if (row != null && row.length >= 2) {
            count = row[0] instanceof Number n ? n.longValue() : 0L;
            total = row[1] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
        }

        return new ItineraryAggregateDTO(count, total);
    }
}

