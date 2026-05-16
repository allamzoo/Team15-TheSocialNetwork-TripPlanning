package com.team15.tripplanning.itineraryservice.service;

import com.team15.tripplanning.itineraryservice.dto.DestinationRecommendationDTO;
import com.team15.tripplanning.itineraryservice.dto.ItineraryAnalyticsDTO;
import com.team15.tripplanning.itineraryservice.dto.TripCostEstimateDTO;
import com.team15.tripplanning.itineraryservice.dto.*;
import com.team15.tripplanning.itineraryservice.model.Itinerary;
import com.team15.tripplanning.itineraryservice.repository.ItineraryRepository;
import com.team15.tripplanning.itineraryservice.repository.VisitGraphRepository;
import com.team15.tripplanning.shared.observer.EntityObserver;
import com.team15.tripplanning.itineraryservice.dto.ItineraryAnalyticsDashboardDTO;

import java.time.LocalDate;
import java.util.List;

import java.util.stream.Collectors;
import com.team15.tripplanning.itineraryservice.dto.ItineraryDayRequestDTO;
import com.team15.tripplanning.itineraryservice.model.ItineraryDay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItineraryService {
    private final ItineraryRepository itineraryRepository;
    private final VisitGraphRepository visitGraphRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final RedisTemplate<String, Object> redisTemplate;

    public ItineraryService(ItineraryRepository itineraryRepository,
                            MongoEventLogger mongoEventLogger,
                            RedisTemplate<String, Object> redisTemplate,
                            VisitGraphRepository visitGraphRepository) {
        this.itineraryRepository = itineraryRepository;
        this.redisTemplate = redisTemplate;
        this.visitGraphRepository = visitGraphRepository;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    private void deleteWildcard(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public Itinerary create(Itinerary itinerary) {
        Itinerary saved = itineraryRepository.save(itinerary);
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", saved.getId());
        payload.put("userId", saved.getUserId());
        notifyObservers("ITINERARY_CREATED", payload);
        deleteWildcard("s3-itineraries::*");
        deleteWildcard("s3-analytics::*");
        return saved;
    }

    @Cacheable(value = "s3-itineraries", key = "'S3::all'")
    public List<Itinerary> findAll() {
        return itineraryRepository.findAll();
    }

    public Itinerary findById(Long id) {
        return itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found: " + id));
    }

    public Itinerary update(Long id, Itinerary itinerary) {
        Itinerary existing = findById(id);
        existing.setUserId(itinerary.getUserId());
        existing.setDestinationId(itinerary.getDestinationId());
        existing.setTitle(itinerary.getTitle());
        existing.setStatus(itinerary.getStatus());
        existing.setEstimatedBudget(itinerary.getEstimatedBudget());
        existing.setMetadata(itinerary.getMetadata());
        existing.setStartDate(itinerary.getStartDate());
        existing.setEndDate(itinerary.getEndDate());
        Itinerary saved = itineraryRepository.save(existing);
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", saved.getId());
        payload.put("userId", saved.getUserId());
        notifyObservers("ITINERARY_UPDATED", payload);
        deleteWildcard("s3-itineraries::*");
        deleteWildcard("s3-details::S3::S3-F5::" + id);
        deleteWildcard("s3-analytics::*");
        return saved;
    }

    public void delete(Long id) {
        Itinerary itinerary = findById(id);
        itineraryRepository.delete(itinerary);
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", id);
        payload.put("userId", itinerary.getUserId());
        notifyObservers("ITINERARY_DELETED", payload);
        deleteWildcard("s3-itineraries::*");
        deleteWildcard("s3-details::S3::S3-F5::" + id);
        deleteWildcard("s3-analytics::*");
    }

    // S3-F1
    public List<Itinerary> searchByStatusAndDateRange(String status, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return itineraryRepository.searchByStatusAndDateRange(status, startDate, endDate);
    }

    @Transactional
    public Itinerary assignDestination(Long itineraryId, Long destinationId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Itinerary not found"));

        if (!Itinerary.ItineraryStatus.DRAFT.equals(itinerary.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT itineraries can be assigned");
        }

        if (itineraryRepository.countDestinationById(destinationId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found");
        }

        if (itineraryRepository.countActiveDestinationById(destinationId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Destination must be ACTIVE");
        }

        itinerary.setDestinationId(destinationId);
        itinerary.setStatus(Itinerary.ItineraryStatus.PLANNED);

        Itinerary saved = itineraryRepository.saveAndFlush(itinerary);
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", saved.getId());
        payload.put("destinationId", destinationId);
        notifyObservers("DESTINATION_ASSIGNED", payload);
        deleteWildcard("s3-itineraries::*");
        deleteWildcard("s3-details::S3::S3-F5::" + itineraryId);
        return saved;
    }

    // S3-F3
    @Cacheable(value = "s3-cost-estimate", key = "'S3::S3-F3::' + #destinationId + '::' + #numberOfDays + '::' + #numberOfTravelers")
    public TripCostEstimateDTO estimateTripCost(Long destinationId, int numberOfDays, int numberOfTravelers) {
        double accommodation = 150.0 * numberOfDays * numberOfTravelers;
        double transport     = 50.0  * numberOfDays * numberOfTravelers;
        double activities    = 100.0 * numberOfDays;

        int activeCount = itineraryRepository.countActiveItinerariesForDestination(destinationId);

        double seasonMultiplier;
        if (activeCount <= 5) {
            seasonMultiplier = 1.0;
        } else if (activeCount <= 15) {
            seasonMultiplier = 1.3;
        } else {
            seasonMultiplier = 1.6;
        }

        double total = (accommodation + transport + activities) * seasonMultiplier;
        return TripCostEstimateDTO.builder()
                .estimatedAccommodation(accommodation)
                .estimatedTransport(transport)
                .estimatedActivities(activities)
                .estimatedTotal(total)
                .seasonMultiplier(seasonMultiplier)
                .build();
    }

    // S3-F4
    @Transactional
    public Itinerary completeItinerary(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Itinerary not found"));

        if (itinerary.getStatus() != Itinerary.ItineraryStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Itinerary must be IN_PROGRESS to complete");
        }

        itinerary.setStatus(Itinerary.ItineraryStatus.COMPLETED);

        if (itinerary.getEstimatedBudget() == null) {
            Double total = itineraryRepository.sumConfirmedBookingsByItinerary(id);
            itinerary.setEstimatedBudget(total != null ? total : 0.0);
        }

        Itinerary saved = itineraryRepository.save(itinerary);
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", saved.getId());
        payload.put("userId", saved.getUserId());
        notifyObservers("ITINERARY_COMPLETED", payload);
        deleteWildcard("s3-itineraries::*");
        deleteWildcard("s3-details::S3::S3-F5::" + id);
        deleteWildcard("s3-analytics::*");
        return saved;
    }

    public List<Itinerary> filterByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "key and value must not be blank");
        }
        return itineraryRepository.findByMetadataKeyValue(key, value);
    }

    @Transactional
    public Itinerary addDays(Long itineraryId, List<ItineraryDayRequestDTO> daysRequest) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));

        if (!(itinerary.getStatus() == Itinerary.ItineraryStatus.DRAFT ||
                itinerary.getStatus() == Itinerary.ItineraryStatus.PLANNED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add days to in-progress or completed itineraries");
        }

        for (ItineraryDayRequestDTO dto : daysRequest) {
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Day title must not be blank");
            }
            if (dto.getDate() == null) {
                dto.setDate(java.time.LocalDate.now());
            }
        }

        int currentMax = 0;
        if (itinerary.getItineraryDays() != null && !itinerary.getItineraryDays().isEmpty()) {
            currentMax = itinerary.getItineraryDays()
                    .stream()
                    .mapToInt(ItineraryDay::getDayOrder)
                    .max()
                    .orElse(0);
        }

        List<ItineraryDay> newDays = new ArrayList<>();

        for (int i = 0; i < daysRequest.size(); i++) {
            ItineraryDayRequestDTO dto = daysRequest.get(i);

            ItineraryDay day = new ItineraryDay();
            day.setDate(dto.getDate());
            day.setTitle(dto.getTitle());
            day.setDescription(dto.getDescription());
            day.setMetadata(dto.getMetadata());
            day.setDayOrder(currentMax + i + 1);
            day.setStatus(ItineraryDay.ItineraryDayStatus.PLANNED);
            day.setItinerary(itinerary);

            newDays.add(day);
        }

        if (itinerary.getItineraryDays() == null) {
            itinerary.setItineraryDays(new ArrayList<>());
        }
        itinerary.getItineraryDays().addAll(newDays);

        Itinerary saved = itineraryRepository.save(itinerary);
        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", saved.getId());
        payload.put("daysAdded", daysRequest.size());
        notifyObservers("DAYS_ADDED", payload);
        deleteWildcard("s3-details::S3::S3-F5::" + itineraryId);
        return saved;
    }

    @Cacheable(value = "s3-details", key = "'S3::S3-F5::' + #id")
    public ItineraryDetailsDTO getItineraryDetails(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Itinerary not found: " + id
                ));

        List<ItineraryDay> days = itinerary.getItineraryDays() != null
                ? itinerary.getItineraryDays()
                : new ArrayList<>();

        days.sort(Comparator.comparingInt(ItineraryDay::getDayOrder));

        List<ItineraryDayDTO> dayDTOs = new ArrayList<>();
        int completedCount = 0;

        for (ItineraryDay day : days) {
            ItineraryDayDTO dto = new ItineraryDayDTO();
            dto.setId(day.getId());
            dto.setDayOrder(day.getDayOrder());
            dto.setDate(day.getDate().toString());
            dto.setTitle(day.getTitle());
            dto.setDescription(day.getDescription());
            dto.setStatus(day.getStatus().name());
            dto.setMetadata(day.getMetadata());

            if (day.getStatus() == ItineraryDay.ItineraryDayStatus.COMPLETED) {
                completedCount++;
            }

            dayDTOs.add(dto);
        }

        return ItineraryDetailsDTO.builder()
                .itineraryId(itinerary.getId())
                .userId(itinerary.getUserId())
                .destinationId(itinerary.getDestinationId())
                .title(itinerary.getTitle())
                .status(itinerary.getStatus().name())
                .estimatedBudget(itinerary.getEstimatedBudget())
                .metadata(itinerary.getMetadata())
                .days(dayDTOs)
                .totalDays(dayDTOs.size())
                .completedDays(completedCount)
                .build();
    }

    @Cacheable(value = "s3-analytics", key = "'S3::S3-F6::' + #startDate + '::' + #endDate")
    public ItineraryAnalyticsDTO getAnalytics(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<Object[]> results = itineraryRepository.getAnalytics(startDate, endDate);

        if (results.isEmpty()) {
            return ItineraryAnalyticsDTO.builder()
                    .totalItineraries(0L)
                    .completedItineraries(0L)
                    .cancelledItineraries(0L)
                    .totalBudget(0.0)
                    .averageBudget(0.0)
                    .completionRate(0.0)
                    .build();
        }

        Object[] row = results.get(0);

        long total           = row[0] != null ? ((Number) row[0]).longValue()   : 0L;
        long completed       = row[1] != null ? ((Number) row[1]).longValue()   : 0L;
        long cancelled       = row[2] != null ? ((Number) row[2]).longValue()   : 0L;
        double totalBudget   = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        double averageBudget = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

        double completionRate = total == 0 ? 0.0 : (completed * 100.0 / total);

        return ItineraryAnalyticsDTO.builder()
                .totalItineraries(total)
                .completedItineraries(completed)
                .cancelledItineraries(cancelled)
                .totalBudget(totalBudget)
                .averageBudget(averageBudget)
                .completionRate(completionRate)
                .build();
    }


    public ItineraryAnalyticsDashboardDTO getItineraryAnalyticsDashboard(
            LocalDate startDate, LocalDate endDate) {

        String cacheKey = "itinerary-service::S3-F10::" + startDate + "::" + endDate;

        // 1. Try Redis cache
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        ItineraryAnalyticsDashboardDTO dashboard;

        if (cached != null) {
            @SuppressWarnings("unchecked")
            Map<String, Long> statusMap = (Map<String, Long>) cached.get("itinerariesByStatus");
            dashboard = ItineraryAnalyticsDashboardDTO.builder()
                    .totalItineraries(((Number) cached.get("totalItineraries")).longValue())
                    .totalBudget(((Number) cached.get("totalBudget")).doubleValue())
                    .averageBudget(((Number) cached.get("averageBudget")).doubleValue())
                    .completionRate(((Number) cached.get("completionRate")).doubleValue())
                    .itinerariesByStatus(statusMap)
                    .build();
        } else {
            // 2. Query database
            List<Object[]> rows = itineraryRepository.findStatusCountsAndBudgetSumByDateRange(startDate, endDate);

            long totalItineraries = 0L;
            double totalBudget = 0.0;
            Map<String, Long> statusCounts = new HashMap<>();

            for (Object[] row : rows) {
                String status = (String) row[0];
                long count = ((Number) row[1]).longValue();
                double sumBudget = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

                totalItineraries += count;
                totalBudget += sumBudget;
                statusCounts.put(status, count);
            }

            // Ensure all statuses appear
            for (String s : List.of("DRAFT", "PLANNED", "IN_PROGRESS", "COMPLETED", "CANCELLED")) {
                statusCounts.putIfAbsent(s, 0L);
            }

            double averageBudget = totalItineraries > 0 ? totalBudget / totalItineraries : 0.0;
            long completed = statusCounts.getOrDefault("COMPLETED", 0L);
            double completionRate = totalItineraries > 0 ? (double) completed / totalItineraries : 0.0;

            dashboard = ItineraryAnalyticsDashboardDTO.builder()
                    .totalItineraries(totalItineraries)
                    .totalBudget(totalBudget)
                    .averageBudget(averageBudget)
                    .completionRate(completionRate)
                    .itinerariesByStatus(statusCounts)
                    .build();

            // Store in Redis, 10 minutes TTL
            Map<String, Object> cacheMap = new HashMap<>();
            cacheMap.put("totalItineraries", totalItineraries);
            cacheMap.put("totalBudget", totalBudget);
            cacheMap.put("averageBudget", averageBudget);
            cacheMap.put("completionRate", completionRate);
            cacheMap.put("itinerariesByStatus", statusCounts);
            redisTemplate.opsForValue().set(cacheKey, cacheMap, 10, TimeUnit.MINUTES);
        }

        // 3. Log ANALYTICS_VIEWED event (every call)
        Map<String, Object> details = new HashMap<>();
        details.put("startDate", startDate.toString());
        details.put("endDate", endDate.toString());
        details.put("source", "S3-F10");

        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", 0L);
        payload.put("action", "ANALYTICS_VIEWED");
        payload.put("details", details);

        notifyObservers("ANALYTICS_VIEWED", payload);

        return dashboard;
    }

    @Transactional
    public Itinerary cancelItinerary(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));

        if (itinerary.getStatus() == Itinerary.ItineraryStatus.COMPLETED
                || itinerary.getStatus() == Itinerary.ItineraryStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT or PLANNED itineraries can be cancelled");
        }

        if (itinerary.getStatus() != Itinerary.ItineraryStatus.CANCELLED) {
            itinerary.setStatus(Itinerary.ItineraryStatus.CANCELLED);
            itineraryRepository.cancelPendingBookings(id);
            itinerary = itineraryRepository.save(itinerary);

            Map<String, Object> payload = new HashMap<>();
            payload.put("itineraryId", itinerary.getId());
            payload.put("userId", itinerary.getUserId());
            notifyObservers("ITINERARY_CANCELLED", payload);
            deleteWildcard("s3-itineraries::*");
            deleteWildcard("s3-details::S3::S3-F5::" + id);
            deleteWildcard("s3-analytics::*");
        }

        return itinerary;
    }

    @Transactional
    public Map<String, Object> recordVisit(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));

        if (itinerary.getStatus() != Itinerary.ItineraryStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only COMPLETED itineraries can record visits");
        }

        if (itinerary.getDestinationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itinerary has no destination assigned");
        }

        List<Object[]> userRows = itineraryRepository.findUserInfoForVisit(itineraryId);
        if (userRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for itinerary");
        }

        List<Object[]> destinationRows = itineraryRepository.findDestinationInfoForVisit(itineraryId);
        if (destinationRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found for itinerary");
        }

        Object[] userRow = userRows.get(0);
        Long userId = ((Number) userRow[0]).longValue();
        String userName = userRow[1] != null ? userRow[1].toString() : "Unknown User";

        Object[] destinationRow = destinationRows.get(0);
        Long destinationId = ((Number) destinationRow[0]).longValue();
        String destinationName = destinationRow[1] != null ? destinationRow[1].toString() : "Unknown Destination";
        String country = destinationRow[2] != null ? destinationRow[2].toString() : "";
        String category = destinationRow[3] != null ? destinationRow[3].toString() : "";

        boolean alreadyRecorded = visitGraphRepository.isItineraryAlreadyRecorded(userId, destinationId, itineraryId);

        if (alreadyRecorded) {
            long visitCount = visitGraphRepository.getVisitCount(userId, destinationId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Visit already recorded");
            response.put("itineraryId", itineraryId);
            response.put("userId", userId);
            response.put("destinationId", destinationId);
            response.put("visitCount", visitCount);
            response.put("idempotent", true);
            return response;
        }

        long visitCount = visitGraphRepository.recordVisit(
                userId,
                userName,
                destinationId,
                destinationName,
                country,
                category,
                itineraryId
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("itineraryId", itineraryId);
        payload.put("userId", userId);
        payload.put("destinationId", destinationId);
        notifyObservers("VISIT_RECORDED", payload);

        deleteWildcard("itinerary-service::S3-F12::*");
        deleteWildcard("s3-recommendations::*");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Visit recorded successfully");
        response.put("itineraryId", itineraryId);
        response.put("userId", userId);
        response.put("destinationId", destinationId);
        response.put("visitCount", visitCount);
        response.put("idempotent", false);
        return response;
    }

    // S3-F12
    @Cacheable(value = "s3-recommendations", key = "'S3::S3-F12::' + #userId + '::' + #limit")
    public List<DestinationRecommendationDTO> getRecommendations(Long userId, int limit) {
        if (itineraryRepository.countUsersById(userId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }

        List<long[]> graphResults = visitGraphRepository.getRecommendations(userId, limit);
        if (graphResults.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> destIds = graphResults.stream()
                .map(r -> r[0])
                .collect(Collectors.toList());

        List<Object[]> destRows = itineraryRepository.findDestinationDetailsByIds(destIds);

        Map<Long, Object[]> destMap = new HashMap<>();
        for (Object[] row : destRows) {
            destMap.put(((Number) row[0]).longValue(), row);
        }

        List<DestinationRecommendationDTO> result = new ArrayList<>();
        for (long[] graphRow : graphResults) {
            Long destId = graphRow[0];
            Long score = graphRow[1];
            Object[] dest = destMap.get(destId);
            if (dest != null) {
                result.add(DestinationRecommendationDTO.builder()
                        .destinationId(destId)
                        .name(dest[1] != null ? dest[1].toString() : "")
                        .country(dest[2] != null ? dest[2].toString() : "")
                        .category(dest[3] != null ? dest[3].toString() : "")
                        .score(score)
                        .build());
            }
        }

        return result;
    }

    // ── Internal service-to-service helpers (S1-F4, S1-F9) ─────────────────
    public int getActiveCountForUser(Long userId) {
        return (int) itineraryRepository.findByUserId(userId).stream()
                .filter(i -> i.getStatus() == Itinerary.ItineraryStatus.DRAFT
                          || i.getStatus() == Itinerary.ItineraryStatus.PLANNED
                          || i.getStatus() == Itinerary.ItineraryStatus.IN_PROGRESS)
                .count();
    }

    public long getCompletedCountForUser(Long userId) {
        return itineraryRepository.findByUserId(userId).stream()
                .filter(i -> i.getStatus() == Itinerary.ItineraryStatus.COMPLETED)
                .count();
    }

    public java.util.Map<String, Object> getSummaryForUser(Long userId) {
        var list = itineraryRepository.findByUserId(userId);
        long total     = list.size();
        long completed = list.stream().filter(i -> i.getStatus() == Itinerary.ItineraryStatus.COMPLETED).count();
        long cancelled = list.stream().filter(i -> i.getStatus() == Itinerary.ItineraryStatus.CANCELLED).count();
        return java.util.Map.of("totalTrips", total, "completedTrips", completed, "cancelledTrips", cancelled);
    }
}
