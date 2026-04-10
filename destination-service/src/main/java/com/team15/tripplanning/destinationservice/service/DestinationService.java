package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;
import com.team15.tripplanning.destinationservice.dto.DestinationRevenueDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    public List<com.team15.tripplanning.destinationservice.dto.TopDestinationDTO> getTopRatedDestinations(int limit) {
        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than 0");
        }

        List<Object[]> rows = destinationRepository.findTopRatedWithBookingCount(limit);

        return rows.stream().map(row -> new com.team15.tripplanning.destinationservice.dto.TopDestinationDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                row[2] == null ? 0.0 : ((Number) row[2]).doubleValue(),
                row[3] == null ? 0L : ((Number) row[3]).longValue()
        )).toList();
    }

}
