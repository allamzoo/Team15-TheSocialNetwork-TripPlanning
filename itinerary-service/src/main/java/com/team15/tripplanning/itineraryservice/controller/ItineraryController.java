package com.team15.tripplanning.itineraryservice.controller;

import com.team15.tripplanning.contracts.dto.BatchItineraryRequest;
import com.team15.tripplanning.contracts.dto.DestinationBookingRevenueAggregateDTO;
import com.team15.tripplanning.contracts.dto.DestinationDashboardAggregateDTO;
import com.team15.tripplanning.contracts.dto.ItinerarySummaryDTO;
import com.team15.tripplanning.contracts.dto.UserTripSummaryAggregateDTO;
import com.team15.tripplanning.itineraryservice.dto.DestinationRecommendationDTO;
import com.team15.tripplanning.itineraryservice.dto.EstimateRequest;
import com.team15.tripplanning.itineraryservice.dto.ItineraryAnalyticsDTO;
import com.team15.tripplanning.itineraryservice.dto.TripCostEstimateDTO;
import com.team15.tripplanning.itineraryservice.model.Itinerary;
import com.team15.tripplanning.itineraryservice.model.ItineraryDay;
import com.team15.tripplanning.itineraryservice.security.JwtService;
import com.team15.tripplanning.itineraryservice.service.ItineraryService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import com.team15.tripplanning.itineraryservice.dto.ItineraryAnalyticsDashboardDTO;

@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {
    private final ItineraryService itineraryService;
    private final JwtService jwtService;

    public ItineraryController(ItineraryService itineraryService, JwtService jwtService) {
        this.itineraryService = itineraryService;
        this.jwtService = jwtService;
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
    @PutMapping("/{id}/assign")
    public ResponseEntity<Itinerary> assignDestination(
            @PathVariable Long id,
            @RequestParam Long destinationId) {

        Itinerary result = itineraryService.assignDestination(id, destinationId);
        return ResponseEntity.ok(result);
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
    @GetMapping("/{itineraryId}/days")
    public ResponseEntity<List<ItineraryDay>> getDays(@PathVariable Long itineraryId) {
        Itinerary itinerary = itineraryService.findById(itineraryId);

        List<ItineraryDay> days = itinerary.getItineraryDays();
        if (days == null) {
            days = new java.util.ArrayList<>();
        }

        return ResponseEntity.ok(days);
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

    @PostMapping("/{itineraryId}/record-visit")
    public ResponseEntity<Map<String, Object>> recordVisit(@PathVariable Long itineraryId) {
        return ResponseEntity.ok(itineraryService.recordVisit(itineraryId));
    }

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<ItineraryAnalyticsDashboardDTO> getAnalyticsDashboard(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }

        ItineraryAnalyticsDashboardDTO dashboard = itineraryService.getItineraryAnalyticsDashboard(startDate, endDate);
        return ResponseEntity.ok(dashboard);
    }

    // ── M3 aggregate endpoints (called by S1, S2, S5 via Feign) ─────────────────

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<UserTripSummaryAggregateDTO> getUserTripSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(itineraryService.getUserTripSummary(userId));
    }

    @GetMapping("/user/{userId}/active-count")
    public ResponseEntity<Integer> getUserActiveCount(@PathVariable Long userId) {
        return ResponseEntity.ok(itineraryService.getUserActiveCount(userId));
    }

    @GetMapping("/user/{userId}/completed-count")
    public ResponseEntity<Long> getUserCompletedCount(@PathVariable Long userId) {
        return ResponseEntity.ok(itineraryService.getUserCompletedCount(userId));
    }

    @GetMapping("/destination/{destinationId}/booking-revenue")
    public ResponseEntity<DestinationBookingRevenueAggregateDTO> getDestinationBookingRevenue(
            @PathVariable Long destinationId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(itineraryService.getDestinationBookingRevenue(destinationId, startDate, endDate));
    }

    @GetMapping("/destination/{destinationId}/active-count")
    public ResponseEntity<Integer> getDestinationActiveCount(@PathVariable Long destinationId) {
        return ResponseEntity.ok(itineraryService.getDestinationActiveCount(destinationId));
    }

    @GetMapping("/destination/{destinationId}/dashboard-aggregate")
    public ResponseEntity<DestinationDashboardAggregateDTO> getDestinationDashboardAggregate(
            @PathVariable Long destinationId) {
        return ResponseEntity.ok(itineraryService.getDestinationDashboardAggregate(destinationId));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ItinerarySummaryDTO>> batchGetItineraries(
            @RequestBody BatchItineraryRequest request) {
        return ResponseEntity.ok(itineraryService.batchGetItineraries(request.itineraryIds()));
    }

    // S3-F12
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Long callerId = jwtService.extractUserId(token);
        String callerRole = jwtService.extractRole(token);

        if (!userId.equals(callerId) && !"ADMIN".equals(callerRole)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        List<DestinationRecommendationDTO> recommendations = itineraryService.getRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }
}