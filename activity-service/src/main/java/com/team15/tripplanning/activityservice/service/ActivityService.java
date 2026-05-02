package com.team15.tripplanning.activityservice.service;

import com.team15.tripplanning.activityservice.dto.*;
import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEventKey;
import com.team15.tripplanning.activityservice.repository.ActivityLifecycleEventStore;
import com.team15.tripplanning.activityservice.repository.ActivityRepository;
import com.team15.tripplanning.shared.observer.EntityObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityService {
    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityRepository activityRepository;
    private final ActivityLifecycleEventStore lifecycleEventRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Set<String> VALID_LIFECYCLE_STATUSES =
            Set.of("BOOKED", "STARTED", "COMPLETED", "CANCELLED");

    public ActivityService(ActivityRepository activityRepository,
                           ActivityLifecycleEventStore lifecycleEventRepository,
                           MongoEventLogger mongoEventLogger,
                           RedisTemplate<String, Object> redisTemplate) {
        this.activityRepository = activityRepository;
        this.lifecycleEventRepository = lifecycleEventRepository;
        this.redisTemplate = redisTemplate;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Map<String, Object> payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    private void deleteWildcard(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Cache eviction skipped (Redis unavailable) for pattern {}: {}", pattern, e.getMessage());
        }
    }

    public Activity create(Activity activity) {
        Activity saved = activityRepository.save(activity);
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", saved.getId());
        payload.put("itineraryId", saved.getItineraryId());
        notifyObservers("ACTIVITY_CREATED", payload);
        deleteWildcard("s4-activities::*");
        deleteWildcard("s4-f1-latest::S4::S4-F1::" + saved.getItineraryId());
        deleteWildcard("s4-f8-summary::S4::S4-F8::" + saved.getItineraryId() + "*");
        return saved;
    }

    @Cacheable(value = "s4-activities", key = "'S4::all'")
    public List<Activity> findAll() {
        return activityRepository.findAll();
    }

    public Activity findById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found: " + id));
    }

    public Activity update(Long id, Activity activity) {
        Activity existing = findById(id);
        if (activity.getItineraryId() != null) {
            existing.setItineraryId(activity.getItineraryId());
        }
        if (activity.getName() != null) {
            existing.setName(activity.getName());
        }
        if (activity.getCategory() != null) {
            existing.setCategory(activity.getCategory());
        }
        if (activity.getLatitude() != null) {
            existing.setLatitude(activity.getLatitude());
        }
        if (activity.getLongitude() != null) {
            existing.setLongitude(activity.getLongitude());
        }
        if (activity.getScheduledTime() != null) {
            existing.setScheduledTime(activity.getScheduledTime());
        }
        if (activity.getMetadata() != null) {
            existing.setMetadata(activity.getMetadata());
        }
        Activity saved = activityRepository.save(existing);
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", saved.getId());
        payload.put("itineraryId", saved.getItineraryId());
        notifyObservers("ACTIVITY_UPDATED", payload);
        deleteWildcard("s4-activities::*");
        deleteWildcard("s4-f1-latest::S4::S4-F1::" + saved.getItineraryId());
        deleteWildcard("s4-f8-summary::S4::S4-F8::" + saved.getItineraryId() + "*");
        deleteWildcard("s4-f9-budget::*");
        return saved;
    }

    @Transactional
    public Activity createActivityForItinerary(Long itineraryId, Activity activity) {
        if (!activityRepository.itineraryExists(itineraryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
        activity.setItineraryId(itineraryId);
        Activity saved = activityRepository.save(activity);
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", saved.getId());
        payload.put("itineraryId", itineraryId);
        notifyObservers("ACTIVITY_CREATED", payload);
        deleteWildcard("s4-activities::*");
        deleteWildcard("s4-f1-latest::S4::S4-F1::" + itineraryId);
        deleteWildcard("s4-f8-summary::S4::S4-F8::" + itineraryId + "*");
        return saved;
    }

    public List<NearbyActivityDTO> findNearbyActivities(Double lat, Double lon, Double radiusKm) {
        if (lat == null || lat < -90 || lat > 90 ||
                lon == null || lon < -180 || lon > 180 ||
                radiusKm == null || radiusKm <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid latitude, longitude, or radius");
        }

        List<Object[]> rows = activityRepository.findNearbyActivitiesRaw(lat, lon, radiusKm);
        return rows.stream()
                .map(row -> NearbyActivityDTO.builder()
                        .activityId(((Number) row[0]).longValue())
                        .name((String) row[1])
                        .category((String) row[2])
                        .latitude((Double) row[3])
                        .longitude((Double) row[4])
                        .distanceKm((Double) row[5])
                        .build())
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        Activity activity = findById(id);
        activityRepository.delete(activity);
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", id);
        payload.put("itineraryId", activity.getItineraryId());
        notifyObservers("ACTIVITY_DELETED", payload);
        deleteWildcard("s4-activities::*");
        deleteWildcard("s4-f1-latest::S4::S4-F1::" + activity.getItineraryId());
        deleteWildcard("s4-f8-summary::S4::S4-F8::" + activity.getItineraryId() + "*");
        deleteWildcard("s4-f9-budget::*");
    }

    // ---------- S4-F1 ----------
    @Cacheable(value = "s4-f1-latest", key = "'S4::S4-F1::' + #itineraryId")
    public Activity getLatestActivityForItinerary(Long itineraryId) {
        if (!activityRepository.itineraryExists(itineraryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }
        return activityRepository.findFirstByItineraryIdOrderByScheduledTimeDesc(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No activities found for this itinerary"));
    }

    // ---------- S4-F9 ----------
    @Cacheable(value = "s4-f9-budget", key = "'S4::S4-F9::' + #maxCost + '::' + #sinceMinutes")
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

            return BudgetActivityDTO.builder()
                    .activityId(((Number) row[0]).longValue())
                    .name((String) row[1])
                    .category((String) row[2])
                    .latitude(((Number) row[3]).doubleValue())
                    .longitude(((Number) row[4]).doubleValue())
                    .cost(((Number) row[5]).doubleValue())
                    .scheduledTime(scheduledTime)
                    .build();
        }).collect(Collectors.toList());
    }

    // ---------- S4-F8 ----------
    @Cacheable(value = "s4-f8-summary", key = "'S4::S4-F8::' + #itineraryId + '::' + #startDate + '::' + #endDate")
    public ActivitySummaryDTO getActivitySummary(Long itineraryId, LocalDate startDate, LocalDate endDate) {
        int exists = activityRepository.countItineraryById(itineraryId);
        if (exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Object[]> results = activityRepository.getActivitySummary(itineraryId, startDateTime, endDateTime);

        if (results == null || results.isEmpty()) {
            return ActivitySummaryDTO.builder()
                    .itineraryId(itineraryId)
                    .totalActivities(0)
                    .averageCost(0.0)
                    .maxCost(0.0)
                    .build();
        }

        Object[] row = results.get(0);

        int total = row[0] != null ? ((Number) row[0]).intValue() : 0;
        Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        Double max = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

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

        return ActivitySummaryDTO.builder()
                .itineraryId(itineraryId)
                .totalActivities(total)
                .averageCost(avg)
                .maxCost(max)
                .firstScheduledTime(first)
                .lastScheduledTime(last)
                .build();
    }

    // ---------- S4-F7 ----------
    @Transactional
    public int purgeOldActivities(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        int count = activityRepository.countByScheduledTimeBefore(cutoff);
        activityRepository.deleteByScheduledTimeBefore(cutoff);
        Map<String, Object> payload = new HashMap<>();
        payload.put("olderThanDays", olderThanDays);
        payload.put("purgedCount", count);
        notifyObservers("OLD_DATA_PURGED", payload);
        deleteWildcard("s4-activities::*");
        deleteWildcard("s4-f1-latest::*");
        deleteWildcard("s4-f8-summary::*");
        deleteWildcard("s4-f9-budget::*");
        return count;
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

        List<Activity> saved = activityRepository.saveAll(activities);
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", itineraryId);
        payload.put("count", saved.size());
        notifyObservers("ACTIVITIES_BATCH_CREATED", payload);
        deleteWildcard("s4-activities::*");
        deleteWildcard("s4-f1-latest::S4::S4-F1::" + itineraryId);
        deleteWildcard("s4-f8-summary::S4::S4-F8::" + itineraryId + "*");
        return saved;
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

    // ---------- S4-F5 ----------
    public List<Activity> filterActivityByMetadata(String key, String operator, String value) {
        if (!operator.equals("eq") && !operator.equals("gt") && !operator.equals("lt")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid operator: " + operator + ". Allowed operators are: eq, gt, lt");
        }

        List<Activity> results;
        switch (operator) {
            case "eq":
                results = activityRepository.filterByMetadataEqNative(key, value);
                break;
            case "gt":
                try {
                    Double.parseDouble(value);
                    results = activityRepository.filterByMetadataGtNative(key, value);
                } catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Value must be numeric for 'gt' operator");
                }
                break;
            case "lt":
                try {
                    Double.parseDouble(value);
                    results = activityRepository.filterByMetadataLtNative(key, value);
                } catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Value must be numeric for 'lt' operator");
                }
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid operator: " + operator);
        }

        return results;
    }

    // ---------- S4-F6 ----------
    public List<Activity> getActivitiesInDateRange(java.time.LocalDateTime startDate,
                                                     java.time.LocalDateTime endDate,
                                                     Activity.ActivityCategory category) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate and endDate are required parameters");
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be before or equal to endDate");
        }

        if (category != null) {
            return activityRepository.findActivitiesByDateRangeAndCategory(startDate, endDate, String.valueOf(category));
        } else {
            return activityRepository.findActivitiesInDateRange(startDate, endDate);
        }
    }

    // ---------- S4-F10 ----------
    public ActivityAnalyticsDTO getActivityAnalytics(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must not be after endDate");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("startDate", startDate.toString());
        payload.put("endDate",   endDate.toString());
        notifyObservers("ANALYTICS_VIEWED", payload);

        return getActivityAnalyticsCached(startDate, endDate);
    }

    @Cacheable(value = "s4-f10-analytics", key = "'S4::S4-F10::' + #startDate + '::' + #endDate")
    public ActivityAnalyticsDTO getActivityAnalyticsCached(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime   = endDate.atTime(23, 59, 59, 999_000_000);

        Object[] summary = activityRepository.getAnalyticsSummary(startDateTime, endDateTime);
        int    total            = summary[0] != null ? ((Number) summary[0]).intValue()    : 0;
        double avgCost          = summary[1] != null ? ((Number) summary[1]).doubleValue() : 0.0;
        double avgDurationHours = summary[2] != null ? ((Number) summary[2]).doubleValue() : 0.0;

        List<Object[]> categoryRows = activityRepository.getCountByCategory(startDateTime, endDateTime);
        Map<String, Long> activitiesByCategory = new java.util.LinkedHashMap<>();
        for (Object[] row : categoryRows) {
            String cat = (String) row[0];
            Long   cnt = ((Number) row[1]).longValue();
            activitiesByCategory.put(cat, cnt);
        }

        return ActivityAnalyticsDTO.builder()
                .totalActivities(total)
                .averageCost(avgCost)
                .averageDurationHours(avgDurationHours)
                .activitiesByCategory(activitiesByCategory)
                .build();
    }

    // ---------- S4-F11 ----------
    public ActivityLifecycleEventDTO recordLifecycleEvent(Long activityId, RecordEventRequest request) {
        // b) Find activity in PostgreSQL — throws 404 if not found
        Activity activity = findById(activityId);

        // c) Validate status
        String rawStatus = request.getStatus();
        if (rawStatus == null || !VALID_LIFECYCLE_STATUSES.contains(rawStatus.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be one of: BOOKED, STARTED, COMPLETED, CANCELLED");
        }
        String status = rawStatus.toUpperCase();

        // d) Store lifecycle event in Cassandra
        Instant now = Instant.now();
        ActivityLifecycleEventKey key = new ActivityLifecycleEventKey(activityId, now);
        ActivityLifecycleEvent event = new ActivityLifecycleEvent();
        event.setKey(key);
        event.setStatus(status);
        event.setCategory(activity.getCategory().name());
        event.setLatitude(activity.getLatitude());
        event.setLongitude(activity.getLongitude());
        event.setNotes(request.getNotes());
        lifecycleEventRepository.save(event);

        // e) Fire observer → logs EVENT_RECORDED to MongoDB activity_events
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", activityId);
        payload.put("itineraryId", activity.getItineraryId());
        payload.put("status", status);
        payload.put("category", activity.getCategory().name());
        payload.put("notes", request.getNotes());
        notifyObservers("EVENT_RECORDED", payload);

        // f) Return DTO (caller responds with 201)
        return ActivityLifecycleEventDTO.builder()
                .activityId(activityId)
                .eventTimestamp(now)
                .status(status)
                .category(activity.getCategory().name())
                .latitude(activity.getLatitude())
                .longitude(activity.getLongitude())
                .notes(request.getNotes())
                .build();
    }
}
