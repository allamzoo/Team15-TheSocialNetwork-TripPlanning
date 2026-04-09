package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.dto.RateDestinationRequest;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.repository.ItineraryLookupRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DestinationService {
    private final DestinationRepository destinationRepository;
    private final ItineraryLookupRepository itineraryLookupRepository;

    public DestinationService(DestinationRepository destinationRepository,
                              ItineraryLookupRepository itineraryLookupRepository) {
        this.destinationRepository = destinationRepository;
        this.itineraryLookupRepository = itineraryLookupRepository;
    }

    public Destination create(Destination destination) {
        return destinationRepository.save(destination);
    }

    public List<Destination> findAll() {
        return destinationRepository.findAll();
    }

    public Destination findById(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Destination not found with id: " + id
                ));
    }

    public Destination update(Long id, Destination destination) {
        Destination existing = findById(id);
        existing.setName(destination.getName());
        existing.setCountry(destination.getCountry());
        existing.setDescription(destination.getDescription());
        existing.setCategory(destination.getCategory());
        existing.setStatus(destination.getStatus());
        existing.setRating(destination.getRating());
        existing.setTotalRatings(destination.getTotalRatings());
        existing.setDetails(destination.getDetails());
        return destinationRepository.save(existing);
    }

    public void delete(Long id) {
        destinationRepository.delete(findById(id));
    }

    @Transactional
    public Destination rateDestination(Long destinationId, RateDestinationRequest request) {
        validateRating(request);

        Destination destination = findById(destinationId);
        Map<String, Object> itinerary = itineraryLookupRepository.findItineraryById(request.getItineraryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found with id: " + request.getItineraryId()
                ));

        Long linkedDestinationId = extractLong(itinerary.get("destination_id"));
        if (linkedDestinationId == null || !linkedDestinationId.equals(destinationId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Itinerary does not reference this destination"
            );
        }

        Object statusValue = itinerary.get("status");
        if (statusValue == null || !"COMPLETED".equalsIgnoreCase(statusValue.toString())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Itinerary must be COMPLETED before rating the destination"
            );
        }

        int totalRatings = destination.getTotalRatings() != null ? destination.getTotalRatings() : 0;
        double currentRating = destination.getRating() != null ? destination.getRating() : 0.0;
        double newRating = request.getRating().doubleValue();
        double updatedAverage = ((currentRating * totalRatings) + newRating) / (totalRatings + 1);

        destination.setRating(updatedAverage);
        destination.setTotalRatings(totalRatings + 1);
        return destinationRepository.save(destination);
    }

    private void validateRating(RateDestinationRequest request) {
        if (request == null || request.getItineraryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itineraryId is required");
        }
        if (request.getRating() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating is required");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "rating must be between 1 and 5"
            );
        }
    }

    private Long extractLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            return Long.valueOf(value.toString());
        }
        return null;
    }
}
