package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.team15.tripplanning.bookingservice.dto.RevenueReportDTO;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Booking create(Booking booking) {
        return bookingRepository.save(booking);
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
    }

    public Booking update(Long id, Booking booking) {
        Booking existing = findById(id);
        existing.setItineraryId(booking.getItineraryId());
        existing.setUserId(booking.getUserId());
        existing.setAmount(booking.getAmount());
        existing.setType(booking.getType());
        existing.setStatus(booking.getStatus());
        existing.setBookingDetails(booking.getBookingDetails());
        return bookingRepository.save(existing);
    }

    public void delete(Long id) {
        bookingRepository.delete(findById(id));
    }

    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date after end date");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        Object[] result = bookingRepository.getRevenueStats(start, end);

        // 🔥 FIX: unwrap nested array
        Object[] row;
        if (result.length == 1 && result[0] instanceof Object[]) {
            row = (Object[]) result[0];
        } else {
            row = result;
        }

        double totalRevenue = ((Number) row[0]).doubleValue();
        long totalBookings = ((Number) row[1]).longValue();
        double cancelledAmount = ((Number) row[2]).doubleValue();
        long cancelledCount = ((Number) row[3]).longValue();

        double average = totalBookings == 0 ? 0 : totalRevenue / totalBookings;

        return new RevenueReportDTO(
                totalRevenue,
                totalBookings,
                average,
                cancelledAmount,
                cancelledCount
        );
    }
}
