package com.team15.tripplanning.activityservice.service;

import com.team15.tripplanning.activityservice.dto.*;
import com.team15.tripplanning.activityservice.feign.ItineraryServiceClient;
import com.team15.tripplanning.activityservice.messaging.publisher.ActivityEventPublisher;
import com.team15.tripplanning.activityservice.model.Activity;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEvent;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLifecycleEventKey;
import com.team15.tripplanning.activityservice.repository.ActivityLifecycleEventRepository;
import com.team15.tripplanning.activityservice.repository.ActivityLifecycleEventStore;
import com.team15.tripplanning.activityservice.repository.ActivityRepository;
import com.team15.tripplanning.shared.observer.EntityObserver;
import feign.FeignException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private static final Set<String> VALID_LIFECYCLE_STATUSES =
            Set.of("BOOKED", "STARTED", "COMPLETED", "CANCELLED");

    private final ActivityRepository activityRepository;
    private final ActivityLifecycleEventStore lifecycleEventRepository;
    private final CassandraOperations cassandraOperations;
    private final ItineraryServiceClient itineraryServiceClient;
    private final ActivityEventPublisher activityEventPublisher;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final RedisTemplate<String, Object> redisTemplate;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityLifecycleEventStore  lifecycleEventRepository,
                           CassandraOperations cassandraOperations,
                           MongoEventLogger mongoEventLogger,
                           ItineraryServiceClient itineraryServiceClient,
                           RedisTemplate<String, Object> redisTemplate,
                           ActivityEventPublisher activityEventPublisher) {
        this.activityRepository = activityRepository;
        this.lifecycleEventRepository = lifecycleEventRepository;
        this.cassandraOperations = cassandraOperations;
        this.itineraryServiceClient = itineraryServiceClient;
        this.redisTemplate = redisTemplate;
        this.activityEventPublisher = activityEventPublisher;
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

    // S4-READ-DB M3: Feign-based itinerary existence check replaces cross-service SQL
    private void validateItineraryExists(Long itineraryId) {
        try {
            log.info("Calling itinerary-service.getItinerary with args={}", itineraryId);
            itineraryServiceClient.getItinerary(itineraryId);
            log.info("itinerary-service.getItinerary returned successfully for id={}", itineraryId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
        } catch (FeignException e) {
            log.warn("Feign call to itinerary-service failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Itinerary service temporarily unavailable");
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
        if (activity.getItineraryId() != null) existing.setItineraryId(activity.getItineraryId());
        if (activity.getName() != null) existing.setName(activity.getName());
        if (activity.getCategory() != null) existing.setCategory(activity.getCategory());
        if (activity.getLatitude() != null) existing.setLatitude(activity.getLatitude());
        if (activity.getLongitude() != null) existing.setLongitude(activity.getLongitude());
        if (activity.getScheduledTime() != null) existing.setScheduledTime(activity.getScheduledTime());
        if (activity.getMetadata() != null) existing.setMetadata(activity.getMetadata());
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

    // S4-F2 — M3: uses Feign instead of direct SQL; publishes activity.created event
    @Transactional
    public Activity createActivityForItinerary(Long itineraryId, Activity activity) {
        validateItineraryExists(itineraryId);
        activity.setItineraryId(itineraryId);
        Activity saved = activityRepository.save(activity);
        activityEventPublisher.publishActivityCreated(
                saved.getId(),
                itineraryId,
                saved.getCategory().name()
        );
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
    // M3: No Feign needed — 404 derived from no-rows-found (spec §6)
    @Cacheable(value = "s4-f1-latest", key = "'S4::S4-F1::' + #itineraryId")
    public Activity getLatestActivityForItinerary(Long itineraryId) {
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
                scheduledTime = row[6] instanceof LocalDateTime
                        ? (LocalDateTime) row[6]
                        : ((java.sql.Timestamp) row[6]).toLocalDateTime();
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
    // M3: No Feign needed — return zero-count DTO when no rows found (spec §6)
    @Cacheable(value = "s4-f8-summary", key = "'S4::S4-F8::' + #itineraryId + '::' + #startDate + '::' + #endDate")
    public ActivitySummaryDTO getActivitySummary(Long itineraryId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Object[]> results = activityRepository.getActivitySummary(itineraryId, startDateTime, endDateTime);

        if (results == null || results.isEmpty() || results.get(0)[0] == null
                || ((Number) results.get(0)[0]).intValue() == 0) {
            return ActivitySummaryDTO.builder()
                    .itineraryId(itineraryId)
                    .totalActivities(0)
                    .averageCost(0.0)
                    .maxCost(0.0)
                    .build();
        }

        Object[] row = results.get(0);
        int total = ((Number) row[0]).intValue();
        Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        Double max = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

        LocalDateTime first = null;
        LocalDateTime last = null;
        if (row[3] != null) {
            first = row[3] instanceof LocalDateTime
                    ? (LocalDateTime) row[3]
                    : ((java.sql.Timestamp) row[3]).toLocalDateTime();
        }
        if (row[4] != null) {
            last = row[4] instanceof LocalDateTime
                    ? (LocalDateTime) row[4]
                    : ((java.sql.Timestamp) row[4]).toLocalDateTime();
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

    // ---------- S4-F4 — M3: uses Feign instead of direct SQL; publishes activity.created per saved ----------
    @Transactional
    public List<Activity> batchActivityCreation(BatchActivityRequestDTO request) {
        Long itineraryId = request.itineraryId();
        List<Activity> activities = request.activities();
        validateItineraryExists(itineraryId);
        for (Activity activity : activities) {
            validateCoordinates(activity);
        }
        for (Activity activity : activities) {
            activity.setItineraryId(itineraryId);
        }
        List<Activity> saved = activityRepository.saveAll(activities);
        for (Activity s : saved) {
            activityEventPublisher.publishActivityCreated(
                    s.getId(),
                    itineraryId,
                    s.getCategory().name()
            );
        }
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
        switch (operator) {
            case "eq": return activityRepository.filterByMetadataEqNative(key, value);
            case "gt":
                try { Double.parseDouble(value); }
                catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Value must be numeric for 'gt' operator");
                }
                return activityRepository.filterByMetadataGtNative(key, value);
            case "lt":
                try { Double.parseDouble(value); }
                catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Value must be numeric for 'lt' operator");
                }
                return activityRepository.filterByMetadataLtNative(key, value);
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid operator: " + operator);
        }
    }

    // ---------- S4-F6 ----------
    public List<Activity> getActivitiesInDateRange(LocalDateTime startDate,
                                                   LocalDateTime endDate,
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
        }
        return activityRepository.findActivitiesInDateRange(startDate, endDate);
    }

    // ---------- S4-F10 ----------
    public void logAnalyticsViewed(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("startDate", startDate.toString());
        payload.put("endDate", endDate.toString());
        notifyObservers("ANALYTICS_VIEWED", payload);
    }

    @Cacheable(value = "s4-f10-analytics", key = "'S4::S4-F10::' + #startDate + '::' + #endDate")
    public ActivityAnalyticsDTO getActivityAnalytics(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must not be after endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999_000_000);

        List<Object[]> summaryRows = activityRepository.getAnalyticsSummary(startDateTime, endDateTime);
        int total = 0;
        double avgCost = 0.0;
        double avgDurationHours = 0.0;

        if (summaryRows != null && !summaryRows.isEmpty()) {
            Object[] summary = summaryRows.get(0);
            total = summary[0] != null ? ((Number) summary[0]).intValue() : 0;
            avgCost = summary[1] != null ? ((Number) summary[1]).doubleValue() : 0.0;
            avgDurationHours = summary[2] != null ? ((Number) summary[2]).doubleValue() : 0.0;
        }

        List<Object[]> categoryRows = activityRepository.getCountByCategory(startDateTime, endDateTime);
        Map<String, Long> activitiesByCategory = new java.util.LinkedHashMap<>();
        for (Object[] row : categoryRows) {
            activitiesByCategory.put((String) row[0], ((Number) row[1]).longValue());
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
        Activity activity = findById(activityId);

        String rawStatus = request.getStatus();
        if (rawStatus == null || !VALID_LIFECYCLE_STATUSES.contains(rawStatus.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be one of: BOOKED, STARTED, COMPLETED, CANCELLED");
        }
        String status = rawStatus.toUpperCase();

        Instant now = Instant.now();
        ActivityLifecycleEventKey key = new ActivityLifecycleEventKey(activityId, now);
        ActivityLifecycleEvent event = new ActivityLifecycleEvent();
        event.setKey(key);
        event.setStatus(status);
        event.setCategory(activity.getCategory().name());
        event.setLatitude(activity.getLatitude());
        event.setLongitude(activity.getLongitude());
        event.setNotes(request.getNotes());
        cassandraOperations.insert(event);

        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", activityId);
        payload.put("itineraryId", activity.getItineraryId());
        payload.put("status", status);
        payload.put("category", activity.getCategory().name());
        payload.put("notes", request.getNotes());
        notifyObservers("EVENT_RECORDED", payload);

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

    // ---------- S4-F12 ----------
    @Cacheable(value = "s4-f12-timeline", key = "'S4::S4-F12::' + #activityId + '::' + #startTime + '::' + #endTime")
    public List<ActivityEventDTO> getActivityTimeline(Long activityId,
                                                      String startTime,
                                                      String endTime) {
        findById(activityId);

        Instant start = parseFlexibleInstant(startTime);
        Instant end = parseFlexibleInstant(endTime);

        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startTime must be before or equal to endTime");
        }

        List<ActivityLifecycleEvent> events = lifecycleEventRepository.findByActivityId(activityId);

        return events.stream()
                .filter(e -> {
                    Instant ts = e.getKey().getEventTimestamp();
                    if (start != null && ts.isBefore(start)) return false;
                    if (end != null && ts.isAfter(end)) return false;
                    return true;
                })
                .sorted((a, b) -> b.getKey().getEventTimestamp().compareTo(a.getKey().getEventTimestamp()))
                .map(e -> ActivityEventDTO.builder()
                        .timestamp(e.getKey().getEventTimestamp())
                        .status(e.getStatus())
                        .category(e.getCategory())
                        .latitude(e.getLatitude())
                        .longitude(e.getLongitude())
                        .notes(e.getNotes())
                        .build())
                .collect(Collectors.toList());
    }

    private Instant parseFlexibleInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid datetime format. Use 2026-05-02T14:10:00 or 2026-05-02T14:10:00Z");
            }
        }
    }
}