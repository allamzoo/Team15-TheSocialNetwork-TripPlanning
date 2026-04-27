package com.team15.tripplanning.destinationservice.controller;

import com.team15.tripplanning.destinationservice.dto.VerifyReviewRequest;
import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.service.DestinationReviewService;
import com.team15.tripplanning.destinationservice.service.DestinationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DestinationReviewController {
    private final DestinationReviewService destinationReviewService;
    private final DestinationService destinationService;

    public DestinationReviewController(
            DestinationReviewService destinationReviewService,
            DestinationService destinationService
    ) {
        this.destinationReviewService = destinationReviewService;
        this.destinationService = destinationService;
    }

    @PostMapping({"/destination-reviews", "/destinations/{destinationId}/reviews"})
    public ResponseEntity<DestinationReview> create(
            @PathVariable(required = false) Long destinationId,
            @RequestBody DestinationReview destinationReview
    ) {
        DestinationReview created = (destinationId != null)
                ? destinationReviewService.createForDestination(destinationId, destinationReview)
                : destinationReviewService.create(destinationReview);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/destination-reviews")
    public ResponseEntity<List<DestinationReview>> findAll() {
        return ResponseEntity.ok(destinationReviewService.findAll());
    }

    @GetMapping({"/destination-reviews/{id}"})
    public ResponseEntity<DestinationReview> findById(@PathVariable Long id) {
        return ResponseEntity.ok(destinationReviewService.findById(id));
    }

    @GetMapping("/destinations/{destinationId}/reviews")
    public ResponseEntity<List<DestinationReview>> findAllByDestination(@PathVariable Long destinationId) {
        return ResponseEntity.ok(destinationReviewService.findAllByDestinationId(destinationId));
    }

    @GetMapping("/destinations/{destinationId}/reviews/{reviewId}")
    public ResponseEntity<DestinationReview> findByDestinationAndId(
            @PathVariable Long destinationId,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(destinationReviewService.findByDestinationAndReviewId(destinationId, reviewId));
    }

    @PutMapping({"/destination-reviews/{id}"})
    public ResponseEntity<DestinationReview> update(@PathVariable Long id, @RequestBody DestinationReview destinationReview) {
        return ResponseEntity.ok(destinationReviewService.update(id, destinationReview));
    }

    @PutMapping("/destinations/{destinationId}/reviews/{reviewId}")
    public ResponseEntity<DestinationReview> updateForDestination(
            @PathVariable Long destinationId,
            @PathVariable Long reviewId,
            @RequestBody DestinationReview destinationReview
    ) {
        return ResponseEntity.ok(destinationReviewService.updateForDestination(destinationId, reviewId, destinationReview));
    }

    @DeleteMapping({"/destination-reviews/{id}"})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        destinationReviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/destinations/{destinationId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteForDestination(
            @PathVariable Long destinationId,
            @PathVariable Long reviewId
    ) {
        destinationReviewService.deleteByDestinationAndReviewId(destinationId, reviewId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/destinations/{destinationId}/reviews/{reviewId}/verify")
    public ResponseEntity<Void> verifyReviewPut(
            @PathVariable Long destinationId,
            @PathVariable Long reviewId,
            @RequestBody VerifyReviewRequest request
    ) {
        destinationService.verifyReview(destinationId, reviewId, request.getVerifiedBy());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/destinations/{destinationId}/reviews/{reviewId}/verify")
    public ResponseEntity<Void> verifyReviewPost(
            @PathVariable Long destinationId,
            @PathVariable Long reviewId,
            @RequestBody VerifyReviewRequest request
    ) {
        destinationService.verifyReview(destinationId, reviewId, request.getVerifiedBy());
        return ResponseEntity.ok().build();
    }
}
