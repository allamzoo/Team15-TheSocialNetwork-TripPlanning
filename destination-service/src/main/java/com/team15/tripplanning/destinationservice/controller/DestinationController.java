package com.team15.tripplanning.destinationservice.controller;

import com.team15.tripplanning.destinationservice.dto.DestinationRateRequest;
import com.team15.tripplanning.destinationservice.dto.DestinationReviewAlertDTO;
import com.team15.tripplanning.destinationservice.dto.DestinationRevenueDTO;
import com.team15.tripplanning.destinationservice.dto.TopDestinationDTO;
import com.team15.tripplanning.destinationservice.model.Destination;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationSearchDocument;
import com.team15.tripplanning.destinationservice.service.DestinationSearchService;
import com.team15.tripplanning.destinationservice.service.DestinationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {
    private final DestinationService destinationService;
    private final DestinationSearchService destinationSearchService;

    public DestinationController(DestinationService destinationService,
                                 DestinationSearchService destinationSearchService) {
        this.destinationService = destinationService;
        this.destinationSearchService = destinationSearchService;
    }

    @PostMapping
    public ResponseEntity<Destination> create(@RequestBody Destination destination) {
        return ResponseEntity.ok(destinationService.create(destination));
    }

    @GetMapping
    public ResponseEntity<List<Destination>> findAll() {
        return ResponseEntity.ok(destinationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Destination> findById(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Destination> update(@PathVariable Long id, @RequestBody Destination destination) {
        return ResponseEntity.ok(destinationService.update(id, destination));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        destinationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/index")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> indexDestination(@PathVariable Long id) {
        destinationService.indexDestinationExplicit(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/revenue")
    public ResponseEntity<DestinationRevenueDTO> getDestinationRevenueSummary(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(destinationService.getDestinationRevenueSummary(id, startDate, endDate));
    }

    @GetMapping("/search/full-text")
    @PreAuthorize("isAuthenticated()")
    @Cacheable(value = "s2-dest-full-text-search",
               key = "'S2::S2-F10::' + #query + '::' + #category + '::' + #status + '::' + #minRating + '::' + #maxRating")
    public ResponseEntity<List<DestinationSearchDocument>> fullTextSearch(
            @RequestParam String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating
    ) {
        return ResponseEntity.ok(
                destinationSearchService.fullTextSearch(query, category, status, minRating, maxRating));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Destination>> searchDestinations(
            @RequestParam(required = false) DestinationCategory category,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating
    ) {
        return ResponseEntity.ok(destinationService.searchDestinations(category, minRating, maxRating));
    }

    @PutMapping("/{id}/details")
    public ResponseEntity<Destination> updateDestinationDetails(
            @PathVariable Long id,
            @RequestBody Map<String, Object> details
    ) {
        return ResponseEntity.ok(destinationService.updateDestinationDetails(id, details));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Destination> updateDestinationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        return ResponseEntity.ok(destinationService.updateDestinationStatus(id, status));
    }

    @GetMapping("/details/search")
    public ResponseEntity<List<Destination>> filterByDetailAttribute(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(destinationService.filterByDetailAttribute(key, value, status));
    }

    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopDestinationDTO>> getTopRated(@RequestParam int limit) {
        return ResponseEntity.ok(destinationService.getTopRatedDestinations(limit));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Destination> rateDestination(
            @PathVariable Long id,
            @RequestBody DestinationRateRequest request
    ) {
        return ResponseEntity.ok(destinationService.rateDestination(id, request));
    }

    @GetMapping({"/reviews/low-rated", "/low-rated", "/reports/low-rated"})
    public ResponseEntity<List<DestinationReviewAlertDTO>> getLowRatedReviews(
            @RequestParam(defaultValue = "2.0") Double maxRating
    ) {
        return ResponseEntity.ok(destinationService.getLowRatedReviewAlerts(maxRating));
    }
}
