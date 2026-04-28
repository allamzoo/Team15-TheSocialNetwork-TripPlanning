package com.team15.tripplanning.itineraryservice.service;

import com.team15.tripplanning.itineraryservice.model.Itinerary;
import com.team15.tripplanning.itineraryservice.model.ItineraryDay;
import com.team15.tripplanning.itineraryservice.repository.ItineraryDayRepository;
import com.team15.tripplanning.itineraryservice.repository.ItineraryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItineraryDayService {
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryRepository itineraryRepository;

    public ItineraryDayService(
            ItineraryDayRepository itineraryDayRepository,
            ItineraryRepository itineraryRepository
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryRepository = itineraryRepository;
    }

    public ItineraryDay create(ItineraryDay itineraryDay) {
        Long itineraryId = itineraryDay.getItineraryId();
        Itinerary itinerary = resolveItinerary(itineraryId);
        itineraryDay.setItinerary(itinerary);

        // Default title if missing (direct creation endpoint is more lenient)
        if (itineraryDay.getTitle() == null || itineraryDay.getTitle().isBlank()) {
            itineraryDay.setTitle("Day Plan");
        }

        if (itineraryDay.getDayOrder() == null) {
            int maxOrder = itineraryDayRepository.findAll().stream()
                    .filter(d -> itineraryId.equals(d.getItineraryId()))
                    .mapToInt(ItineraryDay::getDayOrder)
                    .max()
                    .orElse(0);
            itineraryDay.setDayOrder(maxOrder + 1);
        }

        if (itineraryDay.getDate() == null) {
            itineraryDay.setDate(java.time.LocalDate.now());
        }

        return itineraryDayRepository.save(itineraryDay);
    }

    public List<ItineraryDay> findAll() {
        return itineraryDayRepository.findAll();
    }

    public ItineraryDay findById(Long id) {
        return itineraryDayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ItineraryDay not found: " + id));
    }

    public ItineraryDay update(Long id, ItineraryDay itineraryDay) {
        ItineraryDay existing = findById(id);
        if (itineraryDay.getItineraryId() != null) {
            existing.setItinerary(resolveItinerary(itineraryDay.getItineraryId()));
        }
        existing.setDayOrder(itineraryDay.getDayOrder());
        existing.setDate(itineraryDay.getDate());
        existing.setTitle(itineraryDay.getTitle());
        existing.setDescription(itineraryDay.getDescription());
        existing.setStatus(itineraryDay.getStatus());
        existing.setMetadata(itineraryDay.getMetadata());
        return itineraryDayRepository.save(existing);
    }

    public void delete(Long id) {
        itineraryDayRepository.delete(findById(id));
    }

    private Itinerary resolveItinerary(Long itineraryId) {
        if (itineraryId == null) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "itineraryId is required");
        }
        return itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Itinerary not found: " + itineraryId));
    }
}
