package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.repository.DestinationReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
    private final DestinationReviewRepository destinationReviewRepository;
    public DestinationService(DestinationRepository destinationRepository,
                              DestinationReviewRepository destinationReviewRepository) {
        this.destinationRepository = destinationRepository;
        this.destinationReviewRepository = destinationReviewRepository;
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
    public Destination verifyReview(Long destinationId, Long reviewId, Long verifierId) {
        // 1. Find Destination (404 if not found)
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));

        // 2. Find Review (404 if not found)
        // You'll need to inject DestinationReviewRepository into this service
        DestinationReview review = destinationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        // 3. Verify relationship (400 if mismatch)
        if (!review.getDestination().getId().equals(destinationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review does not belong to this destination");
        }

        // 4. Check visitDate (400 if in the future)
        if (review.getVisitDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot verify a review for a future visit");
        }

        // 5. Verify Admin status (403 if not Admin)
        String role = destinationRepository.findUserRoleById(verifierId);
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not an Admin");
        }

        // 6. Update review status and JSONB metadata
        review.setVerified(true);
        Map<String, Object> metadata = review.getMetadata();
        metadata.put("verifiedAt", LocalDateTime.now().toString());
        metadata.put("verifiedBy", verifierId);
        review.setMetadata(metadata);

        destinationReviewRepository.save(review);

        // 7. Return updated destination (Hibernate handles the relationship refresh)
        return destinationRepository.findById(destinationId).get();
    }
}
