package com.team15.tripplanning.activityservice.service;

import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.repository.ActivityRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;

import jakarta.transaction.Transactional;
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
    // ---------- S4-F7 ----------
    @Transactional
    public int purgeOldActivities(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        int count = activityRepository.countByScheduledTimeBefore(cutoff);
        activityRepository.deleteByScheduledTimeBefore(cutoff);
        return count;
    }
}
