package com.team15.tripplanning.destinationservice.service;

import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.repository.DestinationRepository;
import com.team15.tripplanning.destinationservice.repository.DestinationReviewRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public DestinationReview createForDestination(Long destinationId, DestinationReview destinationReview) {
        destinationReview.setDestination(resolveDestination(destinationId));
        destinationReview.setDestinationId(destinationId);
        return destinationReviewRepository.save(destinationReview);
    }

    public List<DestinationReview> findAll() {
        return destinationReviewRepository.findAll();
    }

    public List<DestinationReview> findAllByDestinationId(Long destinationId) {
        if (!destinationRepository.existsById(destinationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found: " + destinationId);
        }
        return destinationReviewRepository.findByDestination_Id(destinationId);
    }

    public DestinationReview findById(Long id) {
        return destinationReviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DestinationReview not found: " + id));
    }

    public DestinationReview findByDestinationAndReviewId(Long destinationId, Long reviewId) {
        DestinationReview review = findById(reviewId);
        if (review.getDestination() == null || !destinationId.equals(review.getDestination().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found for destination: " + destinationId);
        }
        return review;
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

    public DestinationReview updateForDestination(Long destinationId, Long reviewId, DestinationReview destinationReview) {
        DestinationReview existing = findByDestinationAndReviewId(destinationId, reviewId);
        destinationReview.setDestinationId(destinationId);
        return update(existing.getId(), destinationReview);
    }

    public void delete(Long id) {
        destinationReviewRepository.delete(findById(id));
    }

    public void deleteByDestinationAndReviewId(Long destinationId, Long reviewId) {
        destinationReviewRepository.delete(findByDestinationAndReviewId(destinationId, reviewId));
    }

    private Destination resolveDestination(Long destinationId) {
        if (destinationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destinationId is required for DestinationReview");
        }
        return destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found: " + destinationId));
    }
}
