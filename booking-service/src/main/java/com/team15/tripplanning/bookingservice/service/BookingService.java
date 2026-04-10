package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import com.team15.tripplanning.bookingservice.dto.UserBookingSummaryDTO;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // ===== CRUD =====
    public Booking create(Booking booking) {
        return bookingRepository.save(booking);
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + id
                ));
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

    // ===== S5-F3: User Booking Summary =====
    public UserBookingSummaryDTO getUserBookingSummary(Long userId) {

        // 1️⃣ Check user exists (based on bookings)
        List<Booking> userBookings = bookingRepository.findByUserId(userId);
        if (userBookings.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        // 2️⃣ Get grouped data (CONFIRMED only)
        List<Object[]> results = bookingRepository.getBookingSummaryByUser(userId);

        Map<String, Double> typeBreakdown = new HashMap<>();
        int totalBookings = 0;
        double totalAmount = 0.0;

        // 3️⃣ Build map + totalAmount
        for (Object[] row : results) {
            String type = row[0].toString();
            Double amount = ((Number) row[1]).doubleValue();

            typeBreakdown.put(type, amount);
            totalAmount += amount;
        }

        // 4️⃣ Count CONFIRMED bookings
        for (Booking b : userBookings) {
            if (b.getStatus() == Booking.BookingStatus.CONFIRMED) {
                totalBookings++;
            }
        }

        // 5️⃣ Return DTO
        return new UserBookingSummaryDTO(
                userId,
                totalBookings,
                totalAmount,
                typeBreakdown
        );
    }
}