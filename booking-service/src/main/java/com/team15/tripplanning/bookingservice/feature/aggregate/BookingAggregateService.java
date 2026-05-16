package com.team15.tripplanning.bookingservice.feature.aggregate;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.contracts.dto.ConfirmedSummaryDTO;
import com.team15.tripplanning.contracts.dto.UserBookingTotalDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BookingAggregateService {

    private final BookingRepository bookingRepository;

    public BookingAggregateService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * S5-F4 / S3-F4 saga pre-check:
     * Returns count and totalRevenue of CONFIRMED bookings for the given itinerary.
     * count >= 1 → saga may proceed.
     * count == 0 → itinerary-service aborts with 400.
     */
    public ConfirmedSummaryDTO getConfirmedSummary(Long itineraryId) {
        // Uses a targeted JPQL aggregate query — never loads the JSON bookingDetails column.
        List<Object[]> rows = bookingRepository.countAndSumConfirmed(itineraryId);
        if (rows.isEmpty()) {
            return new ConfirmedSummaryDTO(0L, BigDecimal.ZERO);
        }
        Object[] row = rows.get(0);
        long      count = ((Number) row[0]).longValue();
        BigDecimal total = row[1] instanceof BigDecimal bd
                ? bd
                : BigDecimal.valueOf(((Number) row[1]).doubleValue());
        return new ConfirmedSummaryDTO(count, total);
    }

    /**
     * S1-F6: sum of CONFIRMED bookings for a user in the date range.
     */
    public UserBookingTotalDTO getUserTotal(Long userId, LocalDate start, LocalDate end) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        BigDecimal total = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .filter(b -> {
                    if (b.getStartDate() == null) return true;
                    return !b.getStartDate().isBefore(start) && !b.getStartDate().isAfter(end);
                })
                .map(b -> BigDecimal.valueOf(b.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .count();
        return new UserBookingTotalDTO(userId, total, count);
    }
}
