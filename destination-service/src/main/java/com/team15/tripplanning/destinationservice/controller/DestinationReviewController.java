package com.team15.tripplanning.destinationservice.controller;

import com.team15.tripplanning.destinationservice.model.DestinationReview;
import com.team15.tripplanning.destinationservice.service.DestinationReviewService;
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
@RequestMapping("/api/destination-reviews")
public class DestinationReviewController {
    private final DestinationReviewService destinationReviewService;

    public DestinationReviewController(DestinationReviewService destinationReviewService) {
        this.destinationReviewService = destinationReviewService;
    }

    @PostMapping
    public ResponseEntity<DestinationReview> create(@RequestBody DestinationReview destinationReview) {
        return ResponseEntity.ok(destinationReviewService.create(destinationReview));
    }

    @GetMapping
    public ResponseEntity<List<DestinationReview>> findAll() {
        return ResponseEntity.ok(destinationReviewService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinationReview> findById(@PathVariable Long id) {
        return ResponseEntity.ok(destinationReviewService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DestinationReview> update(@PathVariable Long id, @RequestBody DestinationReview destinationReview) {
        return ResponseEntity.ok(destinationReviewService.update(id, destinationReview));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        destinationReviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

