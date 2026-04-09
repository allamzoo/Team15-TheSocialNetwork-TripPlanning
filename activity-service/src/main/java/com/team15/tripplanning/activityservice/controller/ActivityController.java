package com.team15.tripplanning.activityservice.controller;

import com.team15.tripplanning.activityservice.dto.ActivitySummaryDTO;
import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.service.ActivityService;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ResponseEntity<Activity> create(@RequestBody Activity activity) {
        return ResponseEntity.ok(activityService.create(activity));
    }

    @GetMapping
    public ResponseEntity<List<Activity>> findAll() {
        return ResponseEntity.ok(activityService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activity> findById(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Activity> update(@PathVariable Long id, @RequestBody Activity activity) {
        return ResponseEntity.ok(activityService.update(id, activity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }
    // ---------- S4-F1 ----------
    @GetMapping("/itinerary/{itineraryId}/latest")
    public ResponseEntity<Activity> getLatestActivity(@PathVariable Long itineraryId) {
        Activity activity = activityService.getLatestActivityForItinerary(itineraryId);
        return ResponseEntity.ok(activity);
    }
    // ---------- S4-F8 ----------
    @GetMapping("/itinerary/{itineraryId}/summary")
    public ResponseEntity<ActivitySummaryDTO> getActivitySummary(
            @PathVariable Long itineraryId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        ActivitySummaryDTO dto = activityService.getActivitySummary(itineraryId, startDate, endDate);
        return ResponseEntity.ok(dto);
    }
}

