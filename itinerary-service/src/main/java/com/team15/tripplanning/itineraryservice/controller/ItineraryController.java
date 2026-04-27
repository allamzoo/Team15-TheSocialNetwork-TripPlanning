package com.team15.tripplanning.itineraryservice.controller;

import com.team15.tripplanning.itineraryservice.dto.EstimateRequest;
import com.team15.tripplanning.itineraryservice.dto.ItineraryAnalyticsDTO;
import com.team15.tripplanning.itineraryservice.dto.TripCostEstimateDTO;
import com.team15.tripplanning.itineraryservice.model.Itinerary;
import com.team15.tripplanning.itineraryservice.service.ItineraryService;
import java.time.LocalDate;
import java.util.List;
import com.team15.tripplanning.itineraryservice.dto.ItineraryDetailsDTO;
import com.team15.tripplanning.itineraryservice.dto.ItineraryDayRequestDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/itineraries")
public class ItineraryController {
    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @PostMapping
    public ResponseEntity<Itinerary> create(@RequestBody Itinerary itinerary) {
        return ResponseEntity.ok(itineraryService.create(itinerary));
    }

    @GetMapping
    public ResponseEntity<List<Itinerary>> findAll() {
        return ResponseEntity.ok(itineraryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Itinerary> findById(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Itinerary> update(@PathVariable Long id, @RequestBody Itinerary itinerary) {
        return ResponseEntity.ok(itineraryService.update(id, itinerary));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itineraryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // S3-F1
    @GetMapping("/search")
    public ResponseEntity<List<Itinerary>> search(
            @RequestParam(required = false) String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(itineraryService.searchByStatusAndDateRange(status, startDate, endDate));
    }
    // S3-F8
    @PostMapping("/{itineraryId}/days")
    public ResponseEntity<?> addDays(
            @PathVariable Long itineraryId,
            @RequestBody List<ItineraryDayRequestDTO> days) {

        try {
            Itinerary updated = itineraryService.addDays(itineraryId, days);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // S3-F2
    @PutMapping("/{itineraryId}/assign")
    public ResponseEntity<Itinerary> assignDestination(
            @PathVariable Long itineraryId,
            @RequestParam Long destinationId
    ) {
        return ResponseEntity.ok(itineraryService.assignDestination(itineraryId, destinationId));
    }

    // S3-F3
    @PostMapping("/estimate")
    public ResponseEntity<TripCostEstimateDTO> estimateTripCost(@RequestBody EstimateRequest request) {
        return ResponseEntity.ok(itineraryService.estimateTripCost(
                request.getDestinationId(),
                request.getNumberOfDays(),
                request.getNumberOfTravelers()
        ));
    }

    @GetMapping("/metadata/search")
    public ResponseEntity<List<Itinerary>> filterByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        return ResponseEntity.ok(itineraryService.filterByMetadata(key, value));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Itinerary> cancelItinerary(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.cancelItinerary(id));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Itinerary> completeItinerary(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.completeItinerary(id));
    }
    // S3-F9
    @GetMapping("/{itineraryId}/details")
    public ResponseEntity<ItineraryDetailsDTO> getItineraryDetails(
            @PathVariable Long itineraryId) {

        ItineraryDetailsDTO details = itineraryService.getItineraryDetails(itineraryId);
        return ResponseEntity.ok(details);
    }
    @GetMapping("/analytics")
    public ResponseEntity<ItineraryAnalyticsDTO> getAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(itineraryService.getAnalytics(startDate, endDate));
    }
}