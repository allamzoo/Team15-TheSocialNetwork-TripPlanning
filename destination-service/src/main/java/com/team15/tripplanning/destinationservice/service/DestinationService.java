package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.dto.DestinationRateRequest;
import com.team15.tripplanning.destinationservice.dto.DestinationReviewAlertDTO;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import jakarta.transaction.Transactional;
import com.team15.tripplanning.destinationservice.repository.DestinationReviewRepository;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.team15.tripplanning.destinationservice.dto.DestinationRevenueDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;

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

    @org.springframework.transaction.annotation.Transactional
    public Destination updateDestinationStatus(Long id, String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        DestinationStatus newStatus;
        try {
            newStatus = DestinationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be one of ACTIVE, SEASONAL, INACTIVE");
        }

        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Destination not found: " + id));

        if (newStatus == DestinationStatus.INACTIVE) {
            long activeItineraries = destinationRepository.countActiveItinerariesForDestination(id);
            if (activeItineraries > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot set destination to INACTIVE: " + activeItineraries + " active itinerary(ies) reference it");
            }
        }

        destination.setStatus(newStatus);
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
    public List<Destination> filterByDetailAttribute(String key, String value, String status) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key and value are required");
        }

        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                normalizedStatus = com.team15.tripplanning.destinationservice.model.DestinationStatus
                        .valueOf(status.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid status. Must be one of ACTIVE, SEASONAL, INACTIVE");
            }
        }

        return destinationRepository.findByDetailAttribute(key, value, normalizedStatus);
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
    @Transactional
    public List<DestinationReviewAlertDTO> getLowRatedReviewAlerts(Double maxRating) {
        return destinationRepository.findAll().stream()
                .map(dest -> {
                    // Filter only the reviews that are <= maxRating
                    List<DestinationReview> lowReviews = dest.getDestinationReviews().stream()
                            .filter(r -> r.getRating() <= maxRating)
                            .toList();

                    // If this destination has low reviews, wrap it in a DTO
                    if (!lowReviews.isEmpty()) {
                        return new DestinationReviewAlertDTO(
                                dest.getId(),
                                dest.getName(),
                                dest.getStatus(),
                                lowReviews
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull) // Remove destinations that had no low reviews
                .toList();
    }
}
