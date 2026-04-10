package com.team15.tripplanning.activityservice.service;

import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.repository.ActivityRepository;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import com.team15.tripplanning.activityservice.dto.BatchActivityRequestDTO;

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

    // ---------- S4-F4 ----------

    @Transactional
    public List<Activity> batchActivitiyCreation(BatchActivityRequestDTO request) {
        Long itineraryId = request.itineraryId();
        List<Activity> activities = request.activities();

        if (!activityRepository.itineraryExists(itineraryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }

        for (Activity activity : activities) {
            validateCoordinates(activity);
        }

        for (Activity activity : activities) {
            activity.setItineraryId(itineraryId);
        }

        return activityRepository.saveAll(activities);
    }
    private void validateCoordinates(Activity activity) {
        Double latitude = activity.getLatitude();
        Double longitude = activity.getLongitude();

        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Latitude must be between -90 and 90");
        }

        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Longitude must be between -180 and 180");
        }
    }
}
