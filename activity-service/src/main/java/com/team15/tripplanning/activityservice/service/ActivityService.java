package com.team15.tripplanning.activityservice.service;

import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import com.team15.tripplanning.activityservice.dto.ActivitySummaryDTO;
import com.team15.tripplanning.activityservice.dto.BudgetActivityDTO;
import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.repository.ActivityRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;


import org.springframework.http.HttpStatus;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ResponseStatusException;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityService {
    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public Activity create(Activity activity) {
        return activityRepository.save(activity);
    }

    public List<Activity> findAll() {
        return activityRepository.findAll();
    }

    public Activity findById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found: " + id));
    }

    public Activity update(Long id, Activity activity) {
        Activity existing = findById(id);
        existing.setItineraryId(activity.getItineraryId());
        existing.setName(activity.getName());
        existing.setCategory(activity.getCategory());
        existing.setLatitude(activity.getLatitude());
        existing.setLongitude(activity.getLongitude());
        existing.setScheduledTime(activity.getScheduledTime());
        existing.setMetadata(activity.getMetadata());
        return activityRepository.save(existing);
    }

    @Transactional
    public Activity createActivityForItinerary(Long itineraryId, Activity activity) {
        if (!activityRepository.itineraryExists(itineraryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
        activity.setItineraryId(itineraryId);
        return activityRepository.save(activity);
    }

    public List<NearbyActivityDTO> findNearbyActivities(Double lat, Double lon, Double radiusKm) {
        // Validate input
        if (lat == null || lat < -90 || lat > 90 ||
                lon == null || lon < -180 || lon > 180 ||
                radiusKm == null || radiusKm <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid latitude, longitude, or radius");
        }

        List<Object[]> rows = activityRepository.findNearbyActivitiesRaw(lat, lon, radiusKm);
        return rows.stream()
                .map(row -> new NearbyActivityDTO(
                        ((Number) row[0]).longValue(),   // activityId
                        (String) row[1],                 // name
                        (String) row[2],                 // category
                        (Double) row[3],                 // latitude
                        (Double) row[4],                 // longitude
                        (Double) row[5]                  // distanceKm
                ))
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        activityRepository.delete(findById(id));
    }

    // ---------- S4-F1 ----------
    public Activity getLatestActivityForItinerary(Long itineraryId) {
        if (!activityRepository.itineraryExists(itineraryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
        return activityRepository.findFirstByItineraryIdOrderByScheduledTimeDesc(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No activities found for this itinerary"));
    }
    // ---------- S4-F9 ----------
    public List<BudgetActivityDTO> findBudgetFriendlyActivities(Double maxCost, int sinceMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(sinceMinutes);
        List<Object[]> results = activityRepository.findBudgetFriendlyActivities(maxCost, since);

        return results.stream().map(row -> {
            LocalDateTime scheduledTime = null;
            if (row[6] != null) {
                if (row[6] instanceof LocalDateTime) {
                    scheduledTime = (LocalDateTime) row[6];
                } else {
                    scheduledTime = ((java.sql.Timestamp) row[6]).toLocalDateTime();
                }
            }

            return new BudgetActivityDTO(
                    ((Number) row[0]).longValue(),      // activityId
                    (String) row[1],                     // name
                    (String) row[2],                     // category
                    ((Number) row[3]).doubleValue(),      // latitude
                    ((Number) row[4]).doubleValue(),      // longitude
                    ((Number) row[5]).doubleValue(),      // cost
                    scheduledTime                         // scheduledTime
            );
        }).collect(java.util.stream.Collectors.toList());
    }
    // ---------- S4-F8 ----------
    public ActivitySummaryDTO getActivitySummary(Long itineraryId, LocalDate startDate, LocalDate endDate) {
        // Verify itinerary exists
        int exists = activityRepository.countItineraryById(itineraryId);
        if (exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Object[]> results = activityRepository.getActivitySummary(itineraryId, startDateTime, endDateTime);

        // Guard: no rows returned at all
        if (results == null || results.isEmpty()) {
            return new ActivitySummaryDTO(itineraryId, 0, 0.0, 0.0, null, null);
        }

        Object[] row = results.get(0);

        int total = row[0] != null ? ((Number) row[0]).intValue() : 0;
        Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        Double max = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

        // Fix: handle both LocalDateTime and Timestamp since driver version affects return type
        LocalDateTime first = null;
        LocalDateTime last = null;

        if (row[3] != null) {
            if (row[3] instanceof LocalDateTime) {
                first = (LocalDateTime) row[3];
            } else {
                first = ((java.sql.Timestamp) row[3]).toLocalDateTime();
            }
        }

        if (row[4] != null) {
            if (row[4] instanceof LocalDateTime) {
                last = (LocalDateTime) row[4];
            } else {
                last = ((java.sql.Timestamp) row[4]).toLocalDateTime();
            }
        }

        return new ActivitySummaryDTO(itineraryId, total, avg, max, first, last);
    }
    // ---------- S4-F7 ----------
    @Transactional
    public int purgeOldActivities(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        int count = activityRepository.countByScheduledTimeBefore(cutoff);
        activityRepository.deleteByScheduledTimeBefore(cutoff);
        return count;
    }
}
