package com.team15.tripplanning.bookingservice.service;

import com.team15.tripplanning.bookingservice.model.Booking;
import com.team15.tripplanning.bookingservice.repository.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    // ===== S5-F2: Cancel Booking =====
    @Transactional
    public Booking cancelBooking(Long id, String reason) {

        // 1️⃣ Find booking (404 if not found)
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found: " + id
                ));

        // 2️⃣ Validate status (400 if not CONFIRMED)
        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only CONFIRMED bookings can be cancelled"
            );
        }

        // 3️⃣ Set status to CANCELLED
        booking.setStatus(Booking.BookingStatus.CANCELLED);

        // 4️⃣ Update bookingDetails (JSONB)
        Map<String, Object> details = booking.getBookingDetails();

        if (details == null) {
            details = new HashMap<>();
        }

        details.put("cancellationReason", reason);
        details.put("cancelledAt", LocalDateTime.now().toString());

        booking.setBookingDetails(details);

        // 5️⃣ Save and return
        return bookingRepository.save(booking);
    }
}