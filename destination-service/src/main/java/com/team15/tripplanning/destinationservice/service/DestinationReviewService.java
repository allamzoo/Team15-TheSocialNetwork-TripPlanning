package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.repository.DestinationReviewRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DestinationReviewService {
    private final DestinationReviewRepository destinationReviewRepository;
    private final DestinationRepository destinationRepository;

    public DestinationReviewService(
            DestinationReviewRepository destinationReviewRepository,
            DestinationRepository destinationRepository
    ) {
        this.destinationReviewRepository = destinationReviewRepository;
        this.destinationRepository = destinationRepository;
    }

    public DestinationReview create(DestinationReview destinationReview) {
        destinationReview.setDestination(resolveDestination(destinationReview.getDestinationId()));
        return destinationReviewRepository.save(destinationReview);
    }

    public List<DestinationReview> findAll() {
        return destinationReviewRepository.findAll();
    }

    public DestinationReview findById(Long id) {
        return destinationReviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DestinationReview not found: " + id));
    }

    public DestinationReview update(Long id, DestinationReview destinationReview) {
        DestinationReview existing = findById(id);
        if (destinationReview.getDestinationId() != null) {
            existing.setDestination(resolveDestination(destinationReview.getDestinationId()));
        }
        existing.setType(destinationReview.getType());
        existing.setContent(destinationReview.getContent());
        existing.setRating(destinationReview.getRating());
        existing.setVisitDate(destinationReview.getVisitDate());
        existing.setVerified(
                destinationReview.getVerified() != null ? destinationReview.getVerified() : existing.getVerified()
        );
        existing.setMetadata(
                destinationReview.getMetadata() != null ? destinationReview.getMetadata() : existing.getMetadata()
        );
        return destinationReviewRepository.save(existing);
    }

    public void delete(Long id) {
        destinationReviewRepository.delete(findById(id));
    }

    private Destination resolveDestination(Long destinationId) {
        if (destinationId == null) {
            throw new RuntimeException("destinationId is required for DestinationReview");
        }
        return destinationRepository.findById(destinationId)
                .orElseThrow(() -> new RuntimeException("Destination not found: " + destinationId));
    }
}
