package com.team15.tripplanning.activityservice.service;

import com.team15.tripplanning.activityservice.dto.BudgetActivityDTO;
import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.repository.ActivityRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;

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
}
