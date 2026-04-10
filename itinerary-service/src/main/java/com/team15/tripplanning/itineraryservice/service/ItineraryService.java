package com.team15.tripplanning.itineraryservice.service;

import com.team15.tripplanning.itineraryservice.dto.TripCostEstimateDTO;
import com.team15.tripplanning.itineraryservice.model.Itinerary;
import com.team15.tripplanning.itineraryservice.repository.ItineraryRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItineraryService {
    private final ItineraryRepository itineraryRepository;

    public ItineraryService(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
    }

    public Itinerary create(Itinerary itinerary) {
        return itineraryRepository.save(itinerary);
    }

    public List<Itinerary> findAll() {
        return itineraryRepository.findAll();
    }

    public Itinerary findById(Long id) {
        return itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
    }

    public Itinerary update(Long id, Itinerary itinerary) {
        Itinerary existing = findById(id);
        existing.setUserId(itinerary.getUserId());
        existing.setDestinationId(itinerary.getDestinationId());
        existing.setTitle(itinerary.getTitle());
        existing.setStatus(itinerary.getStatus());
        existing.setEstimatedBudget(itinerary.getEstimatedBudget());
        existing.setMetadata(itinerary.getMetadata());
        existing.setStartDate(itinerary.getStartDate());
        existing.setEndDate(itinerary.getEndDate());
        return itineraryRepository.save(existing);
    }

    public void delete(Long id) {
        itineraryRepository.delete(findById(id));
    }

    // S3-F1
    public List<Itinerary> searchByStatusAndDateRange(String status, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return itineraryRepository.searchByStatusAndDateRange(status, startDate, endDate);
    }

    // S3-F2
    @Transactional
    public Itinerary assignDestination(Long itineraryId, Long destinationId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));

        if (itinerary.getStatus() != Itinerary.ItineraryStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary must be in DRAFT status to assign a destination");
        }

        if (itineraryRepository.countDestinationById(destinationId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found");
        }

        if (itineraryRepository.countActiveDestinationById(destinationId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination must be ACTIVE");
        }

        itinerary.setDestinationId(destinationId);
        itinerary.setStatus(Itinerary.ItineraryStatus.PLANNED);
        return itineraryRepository.save(itinerary);
    }

    // S3-F3
    public TripCostEstimateDTO estimateTripCost(Long destinationId, int numberOfDays, int numberOfTravelers) {
        double accommodation = 150.0 * numberOfDays * numberOfTravelers;
        double transport     = 50.0  * numberOfDays * numberOfTravelers;
        double activities    = 100.0 * numberOfDays;

        int activeCount = itineraryRepository.countActiveItinerariesForDestination(destinationId);

        double seasonMultiplier;
        if (activeCount <= 5) {
            seasonMultiplier = 1.0;
        } else if (activeCount <= 15) {
            seasonMultiplier = 1.3;
        } else {
            seasonMultiplier = 1.6;
        }

        double total = (accommodation + transport + activities) * seasonMultiplier;
        return new TripCostEstimateDTO(accommodation, transport, activities, total, seasonMultiplier);
    }

    public List<Itinerary> filterByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "key and value must not be blank");
        }
        return itineraryRepository.findByMetadataKeyValue(key, value);
    }
}