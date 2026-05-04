package com.team15.tripplanning.activityservice.controller;

import com.team15.tripplanning.activityservice.dto.ActivityLifecycleEventDTO;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import com.team15.tripplanning.activityservice.dto.ActivityEventDTO;
import com.team15.tripplanning.activityservice.dto.ActivityLifecycleEventDTO;
import com.team15.tripplanning.activityservice.dto.ActivitySummaryDTO;
import com.team15.tripplanning.activityservice.dto.BatchActivityRequestDTO;
import com.team15.tripplanning.activityservice.dto.BatchActivityResponseDTO;
import com.team15.tripplanning.activityservice.dto.BudgetActivityDTO;
import com.team15.tripplanning.activityservice.dto.RecordEventRequest;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import com.team15.tripplanning.activityservice.dto.RecordEventRequest;
import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.service.ActivityService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import com.team15.tripplanning.activityservice.dto.BatchActivityRequestDTO;
import com.team15.tripplanning.activityservice.dto.BatchActivityResponseDTO;
import com.team15.tripplanning.activityservice.dto.MetadataFilterResponseDTO;
import org.springframework.web.bind.annotation.*;




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

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyActivityDTO>> findNearby(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Double radiusKm) {
        List<NearbyActivityDTO> result = activityService.findNearbyActivities(lat, lon, radiusKm);
        return ResponseEntity.ok(result);
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
    @PostMapping("/itinerary/{itineraryId}")
    public ResponseEntity<Activity> createActivityForItinerary(
            @PathVariable Long itineraryId,
            @RequestBody Activity activity) {
        Activity created = activityService.createActivityForItinerary(itineraryId, activity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
    // ---------- S4-F7 ----------
    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purgeOldActivities(
            @RequestParam int olderThanDays) {
        int deletedCount = activityService.purgeOldActivities(olderThanDays);
        Map<String, Integer> response = new HashMap<>();
        response.put("deletedCount", deletedCount);
        return ResponseEntity.ok(response);
    }


    // ---------- S4-F9 ----------
    @GetMapping("/budget-friendly")
    public ResponseEntity<List<BudgetActivityDTO>> findBudgetFriendlyActivities(
            @RequestParam Double maxCost,
            @RequestParam int sinceMinutes) {
        List<BudgetActivityDTO> result = activityService.findBudgetFriendlyActivities(maxCost, sinceMinutes);
        return ResponseEntity.ok(result);
    }



    // ---------- S4-F4 ----------
    @PostMapping("/batch")
    public ResponseEntity<BatchActivityResponseDTO> batchActivityCreation(@RequestBody BatchActivityRequestDTO request) {
        List<Activity> createdActivities = activityService.batchActivitiyCreation(request);
        BatchActivityResponseDTO response = new BatchActivityResponseDTO(
                createdActivities.size(),
                "Successfully created " + createdActivities.size() + " activities");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ---------- S4-F5 ----------
    @GetMapping("/metadata/search")
    public ResponseEntity<List<Activity>> searchByMetadata(
            @RequestParam String key,
            @RequestParam String operator,
            @RequestParam String value) {
        return ResponseEntity.ok(activityService.filterActivityByMetadata(key, operator, value));
    }

    // ---------- S4-F6 ----------
    @GetMapping("/history")
    public ResponseEntity<List<Activity>> getActivitiesInDateRange(
            @RequestParam LocalDate  startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Activity.ActivityCategory category) {
        List<Activity> activities = activityService.getActivitiesInDateRange(startDate.atStartOfDay(), endDate.atStartOfDay(), category);
        return ResponseEntity.ok(activities);
    }

    // ---------- S4-F11 ----------
    @PostMapping("/{id}/events")
    public ResponseEntity<ActivityLifecycleEventDTO> recordLifecycleEvent(
            @PathVariable Long id,
            @RequestBody RecordEventRequest request) {
        ActivityLifecycleEventDTO dto = activityService.recordLifecycleEvent(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
    // ---------- S4-F12 ----------
    // ---------- S4-F12 ----------
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<ActivityEventDTO>> getActivityTimeline(
            @PathVariable Long id,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        List<ActivityEventDTO> timeline = activityService.getActivityTimeline(id, startTime, endTime);
        return ResponseEntity.ok(timeline);
    }
}
