package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.dto.DestinationRateRequest;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;
import com.team15.tripplanning.destinationservice.dto.DestinationRevenueDTO;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class DestinationService {
    private final DestinationRepository destinationRepository;

    public DestinationService(DestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
    }

    public Destination create(Destination destination) {
        return destinationRepository.save(destination);
    }

    public List<Destination> findAll() {
        return destinationRepository.findAll();
    }

    public Destination findById(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Destination not found: " + id));
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

    public List<Destination> searchDestinations(DestinationCategory category, Double minRating, Double maxRating) {
        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating cannot be greater than maxRating");
        }

        double min = (minRating != null) ? minRating : 0.0;
        double max = (maxRating != null) ? maxRating : 5.0;

        return destinationRepository.searchDestinations(category, min, max);
    }

    public Destination updateDestinationDetails(Long id, Map<String, Object> incomingDetails) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found: " + id));

        Map<String, Object> existingDetails = destination.getDetails();
        if (existingDetails == null) {
            existingDetails = new HashMap<>();
        }

        if (incomingDetails != null) {
            existingDetails.putAll(incomingDetails);
        }

        destination.setDetails(existingDetails);
        return destinationRepository.save(destination);
    }

    public DestinationRevenueDTO getDestinationRevenueSummary(Long id, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found: " + id));
        List<Object[]> results = destinationRepository.getDestinationRevenueSummary(id, startDate, endDate);
        Long totalBookings = 0L;
        Double totalRevenue = 0.0;
        Double averageBookingAmount = 0.0;

        if (results != null && !results.isEmpty()) {
            Object[] row = results.get(0);

            if (row[0] != null) {
                totalBookings = ((Number) row[0]).longValue();
            }
            if (row[1] != null) {
                totalRevenue = ((Number) row[1]).doubleValue();
            }
            if (row[2] != null) {
                averageBookingAmount = ((Number) row[2]).doubleValue();
            }
        }

        return new DestinationRevenueDTO(
                destination.getId(),
                destination.getName(),
                totalBookings,
                totalRevenue,
                averageBookingAmount
        );
    }
    @Transactional
    public void rateDestination(Long destinationId, DestinationRateRequest request) {
        // 1. Validate rating range (1-5)
        if (request.rating() < 1 || request.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        // 2. Find Destination - Throw 404 if not found
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        // 3. Verify Itinerary existence (Throw 404 if missing)
        if (destinationRepository.countItineraryById(request.itineraryId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }

        // 4. Verify itinerary references destination and is COMPLETED (Throw 400)
        if (destinationRepository.countValidItinerary(request.itineraryId(), destinationId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary is either not completed or for a different destination");
        }

        // 5. Recalculate Running Average
        double currentAvg = (destination.getRating() != null) ? destination.getRating() : 0.0;
        int currentTotal = (destination.getTotalRatings() != null) ? destination.getTotalRatings() : 0;

        double newAvg = ((currentAvg * currentTotal) + request.rating()) / (currentTotal + 1);

        // 6. Update and Save
        destination.setRating(newAvg);
        destination.setTotalRatings(currentTotal + 1);
        destinationRepository.save(destination);
    }


}
