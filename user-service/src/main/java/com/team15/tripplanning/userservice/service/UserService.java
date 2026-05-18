package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.contracts.dto.UserBookingTotalDTO;
import com.team15.tripplanning.contracts.dto.UserTripSummaryAggregateDTO;
import com.team15.tripplanning.contracts.feign.BookingServiceClient;
import com.team15.tripplanning.contracts.feign.ItineraryServiceClient;
import com.team15.tripplanning.userservice.adapter.ObjectArrayDtoAdapter;
import com.team15.tripplanning.userservice.document.AuthEvent;
import com.team15.tripplanning.userservice.dto.ActivityFeedDTO;
import com.team15.tripplanning.userservice.dto.SavedDestinationProfileDTO;
import com.team15.tripplanning.userservice.dto.TopTravelerDTO;
import com.team15.tripplanning.userservice.dto.UserProfileDTO;
import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import com.team15.tripplanning.userservice.messaging.publisher.UserEventPublisher;
import com.team15.tripplanning.userservice.model.Role;
import com.team15.tripplanning.userservice.model.SavedDestination;
import com.team15.tripplanning.userservice.model.Status;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.observer.EntityObserver;
import com.team15.tripplanning.userservice.observer.MongoEventLogger;
import com.team15.tripplanning.userservice.repository.AuthEventRepository;
import com.team15.tripplanning.userservice.repository.UserRepository;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final PasswordEncoder passwordEncoder;
    private final ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthEventRepository authEventRepository;
    private final ItineraryServiceClient itineraryServiceClient;
    private final BookingServiceClient bookingServiceClient;
    private final UserEventPublisher userEventPublisher;

    public UserService(UserRepository userRepository, MongoEventLogger mongoEventLogger,
                       PasswordEncoder passwordEncoder, RedisTemplate<String, Object> redisTemplate,
                       AuthEventRepository authEventRepository,
                       ItineraryServiceClient itineraryServiceClient,
                       BookingServiceClient bookingServiceClient,
                       UserEventPublisher userEventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.authEventRepository = authEventRepository;
        this.itineraryServiceClient = itineraryServiceClient;
        this.bookingServiceClient = bookingServiceClient;
        this.userEventPublisher = userEventPublisher;
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

    public User create(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        User saved = userRepository.save(user);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", saved.getId());
        notifyObservers("USER_CREATED", payload);
        deleteWildcard("s1-f1-users::*");
        deleteWildcard("s1-f5-top-travelers::*");
        return saved;
    }

    @Cacheable(value = "s1-f1-users", key = "'S1::S1-F1::all'")
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> search(String name, String email, Role role) {
        String normalizedName = normalize(name);
        String normalizedEmail = normalize(email);
        String normalizedRole = role != null ? role.name() : null;
        if (normalizedName == null && normalizedEmail == null && normalizedRole == null) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(normalizedName, normalizedEmail, normalizedRole);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + id
                ));
    }

    @Cacheable(value = "s1-f6-profile", key = "'S1::S1-F6::' + #id")
    public UserProfileDTO getProfile(Long id) {
        User user = userRepository.findByIdWithSavedDestinations(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + id
                ));

        List<SavedDestination> savedDestinations = new ArrayList<>(user.getSavedDestinations());
        savedDestinations.sort(
                Comparator.comparing(
                                (SavedDestination sd) -> Boolean.TRUE.equals(sd.getIsDefault())
                        )
                        .reversed()
                        .thenComparing(SavedDestination::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        List<SavedDestinationProfileDTO> savedDestinationDtos = savedDestinations.stream()
                .map(this::mapSavedDestinationToDto)
                .toList();

        return UserProfileDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .preferences(user.getPreferences())
                .savedDestinations(savedDestinationDtos)
                .totalSavedDestinations((long) savedDestinationDtos.size())
                .build();
    }

    public User update(Long id, User user) {
        User existing = findById(id);

        if (user.getName() != null) existing.setName(user.getName());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getPassword() != null) existing.setPassword(user.getPassword());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getRole() != null) existing.setRole(user.getRole());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        if (user.getPreferences() != null) existing.setPreferences(user.getPreferences());

        User saved = userRepository.save(existing);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", saved.getId());
        notifyObservers("USER_UPDATED", payload);
        deleteWildcard("s1-f1-users::*");
        deleteWildcard("s1-f6-profile::S1::S1-F6::" + id);
        deleteWildcard("s1-f3-trip-summary::S1::S1-F3::" + id);
        deleteWildcard("s1-f12-activity::S1::S1-F12::" + id + "::*");
        return saved;
    }

    public User mergePreferences(Long id, Map<String, Object> newPreferences) {
        User existing = findById(id);
        Map<String, Object> currentPreferences = existing.getPreferences();
        if (currentPreferences == null) {
            currentPreferences = new HashMap<>();
        }
        if (newPreferences != null) {
            currentPreferences.putAll(newPreferences);
        }
        existing.setPreferences(currentPreferences);
        User saved = userRepository.save(existing);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", saved.getId());
        notifyObservers("USER_UPDATED", payload);
        deleteWildcard("s1-f6-profile::S1::S1-F6::" + id);
        deleteWildcard("s1-f8-pref-search::*");
        return saved;
    }

    @Cacheable(value = "s1-f3-trip-summary", key = "'S1::S1-F3::' + #id")
    public UserTripSummaryDTO getTripSummary(Long id) {
        User user = findById(id);
        log.info("Calling itinerary-service.getUserItinerarySummary with args={}", id);
        try {
            UserTripSummaryAggregateDTO summary = itineraryServiceClient.getUserItinerarySummary(id);
            log.info("itinerary-service.getUserItinerarySummary returned successfully");
            return adapter.adapt(user, summary);
        } catch (FeignException.NotFound e) {
            log.info("itinerary-service.getUserItinerarySummary returned successfully");
            return adapter.adapt(user, UserTripSummaryAggregateDTO.empty());
        } catch (FeignException e) {
            log.warn("Feign call to itinerary-service failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Itinerary service temporarily unavailable");
        }
    }

    public void delete(Long id) {
        findById(id);
        userRepository.deleteById(id);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", id);
        notifyObservers("USER_DELETED", payload);
        deleteWildcard("s1-f1-users::*");
        deleteWildcard("s1-f6-profile::S1::S1-F6::" + id);
        deleteWildcard("s1-f3-trip-summary::S1::S1-F3::" + id);
        deleteWildcard("s1-f5-top-travelers::*");
    }

    @Cacheable(value = "s1-f5-top-travelers", key = "'S1::S1-F5::' + #startDate + '::' + #endDate + '::' + #limit")
    public List<TopTravelerDTO> getTopTravelersBySpending(LocalDate startDate, LocalDate endDate, int limit) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }
        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than 0");
        }

        List<User> users = userRepository.findByStatus(Status.ACTIVE);
        String start = startDate.toString();
        String end = endDate.toString();

        List<TopTravelerDTO> result = new ArrayList<>();
        for (User user : users) {
            log.info("Calling booking-service.getUserBookingTotal with args={}", user.getId());
            try {
                UserBookingTotalDTO total = bookingServiceClient.getUserBookingTotal(
                        user.getId(), start, end);
                log.info("booking-service.getUserBookingTotal returned successfully");
                if (total.totalAmount() != null && total.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
                    result.add(TopTravelerDTO.builder()
                            .userId(user.getId())
                            .name(user.getName())
                            .totalSpent(total.totalAmount().doubleValue())
                            .tripCount(total.tripCount())
                            .build());
                }
            } catch (FeignException.NotFound e) {
                log.info("booking-service.getUserBookingTotal returned successfully");
            } catch (FeignException e) {
                log.warn("Feign call to booking-service failed: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Booking service temporarily unavailable");
            }
        }

        return result.stream()
                .sorted(Comparator.comparingDouble(TopTravelerDTO::getTotalSpent).reversed())
                .limit(limit)
                .toList();
    }

    @Cacheable(value = "s1-f8-pref-search", key = "'S1::S1-F8::' + #key + '::' + #value")
    public List<User> searchByPreference(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preference key cannot be blank");
        }
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preference value cannot be blank");
        }
        return userRepository.searchByPreference(key, value);
    }

    @Cacheable(value = "s1-f9-travel-style", key = "'S1::S1-F9::' + #style + '::' + #minTrips")
    public List<User> findByTravelStyleWithMinimumTrips(String style, int minTrips) {
        if (style == null || style.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "style cannot be blank");
        }
        if (minTrips < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minTrips cannot be negative");
        }

        List<User> candidates = userRepository.searchByPreference("travelStyle", style);
        List<User> result = new ArrayList<>();
        for (User user : candidates) {
            log.info("Calling itinerary-service.getCompletedItineraryCount with args={}", user.getId());
            try {
                long completedCount = itineraryServiceClient.getCompletedItineraryCount(user.getId());
                log.info("itinerary-service.getCompletedItineraryCount returned successfully");
                if (completedCount >= minTrips) {
                    result.add(user);
                }
            } catch (FeignException e) {
                log.warn("Feign call to itinerary-service failed: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Itinerary service temporarily unavailable");
            }
        }
        return result;
    }

    @Transactional
    public User deactivate(Long id) {
        User user = findById(id);

        log.info("Calling itinerary-service.getActiveItineraryCount with args={}", id);
        try {
            int activeCount = itineraryServiceClient.getActiveItineraryCount(id);
            log.info("itinerary-service.getActiveItineraryCount returned successfully");
            if (activeCount > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot deactivate user with active itineraries"
                );
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (FeignException e) {
            log.warn("Feign call to itinerary-service failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Itinerary service temporarily unavailable");
        }

        user.setStatus(Status.DEACTIVATED);
        User saved = userRepository.save(user);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", saved.getId());
        notifyObservers("USER_DEACTIVATED", payload);
        deleteWildcard("s1-f1-users::*");
        deleteWildcard("s1-f6-profile::S1::S1-F6::" + id);

        userEventPublisher.publishUserDeactivated(saved.getId());
        return saved;
    }

    @Cacheable(value = "s1-f12-activity", key = "'S1::S1-F12::' + #userId + '::' + #page + '::' + #size")
    public Map<String, Object> getActivityFeed(Long userId, int page, int size) {
        findById(userId);
        int cappedSize = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuthEvent> eventPage = authEventRepository.findByUserId(userId, pageable);
        List<ActivityFeedDTO> content = eventPage.getContent().stream()
                .map(e -> ActivityFeedDTO.builder()
                        .userId(userId)
                        .action(e.getAction())
                        .timestamp(e.getTimestamp())
                        .details(e.getDetails())
                        .build())
                .toList();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("content", content);
        result.put("page", page);
        result.put("size", cappedSize);
        result.put("totalElements", eventPage.getTotalElements());
        return result;
    }

    public String health() {
        return "OK";
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TopTravelerDTO mapTopTravelerRow(Object[] row) {
        Long userId = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        String name = row[1] != null ? row[1].toString() : null;
        Double totalSpent = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        Long tripCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        return TopTravelerDTO.builder()
                .userId(userId)
                .name(name)
                .totalSpent(totalSpent)
                .tripCount(tripCount)
                .build();
    }

    private SavedDestinationProfileDTO mapSavedDestinationToDto(SavedDestination sd) {
        return SavedDestinationProfileDTO.builder()
                .label(sd.getLabel())
                .destinationName(sd.getDestinationName())
                .country(sd.getCountry())
                .latitude(sd.getLatitude())
                .longitude(sd.getLongitude())
                .isDefault(sd.getIsDefault())
                .metadata(sd.getMetadata())
                .build();
    }
}
