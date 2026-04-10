package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // ===== CRUD methods =====
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

    // ===== S5-F1: FINAL SAFE LOGIC =====
    public List<Booking> getBookings(String statusStr, LocalDateTime startDateTime, LocalDateTime endDateTime) {

        Booking.BookingStatus status = null;

        if (statusStr != null && !statusStr.isBlank()) {
            status = Booking.BookingStatus.valueOf(statusStr.toUpperCase());
        }

        // Case 1: status + date range
        if (status != null && startDateTime != null && endDateTime != null) {
            return bookingRepository.searchBookings(status, startDateTime, endDateTime);
        }

        //  Case 2: date range only
        if (startDateTime != null && endDateTime != null) {
            return bookingRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
        }

        //  Case 3: status only
        if (status != null) {
            return bookingRepository.findByStatus(status);
        }

        //  Case 4: no filters
        return bookingRepository.findAll();
    }
}