package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.userservice.adapter.ObjectArrayDtoAdapter;
import com.team15.tripplanning.userservice.dto.SavedDestinationProfileDTO;
import com.team15.tripplanning.userservice.dto.TopTravelerDTO;
import com.team15.tripplanning.userservice.dto.UserProfileDTO;
import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import com.team15.tripplanning.userservice.model.Role;
import com.team15.tripplanning.userservice.model.SavedDestination;
import com.team15.tripplanning.userservice.model.Status;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.observer.EntityObserver;
import com.team15.tripplanning.userservice.observer.MongoEventLogger;
import com.team15.tripplanning.userservice.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final PasswordEncoder passwordEncoder;
    private final ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();

    public UserService(UserRepository userRepository, MongoEventLogger mongoEventLogger, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    public User create(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        User saved = userRepository.save(user);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", saved.getId());
        notifyObservers("USER_CREATED", payload);
        return saved;
    }

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
        return saved;
    }

    public UserTripSummaryDTO getTripSummary(Long id) {
        findById(id);
        var result = userRepository.getUserTripSummary(id);
        if (result.isEmpty()) {
            return UserTripSummaryDTO.builder()
                    .userId(id)
                    .totalTrips(0L)
                    .completedTrips(0L)
                    .cancelledTrips(0L)
                    .totalSpent(0.0)
                    .averageBudget(0.0)
                    .build();
        }
        return adapter.adapt(result.get(0));
    }



    public void delete(Long id) {
        findById(id);
        userRepository.deleteById(id);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", id);
        notifyObservers("USER_DELETED", payload);
    }

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

        return userRepository.findTopTravelersBySpending(startDate, endDate, limit)
                .stream()
                .map(this::mapTopTravelerRow)
                .toList();
    }



    public List<User> searchByPreference(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preference key cannot be blank");
        }
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preference value cannot be blank");
        }
        return userRepository.searchByPreference(key, value);
    }

    public List<User> findByTravelStyleWithMinimumTrips(String style, int minTrips) {
        if (style == null || style.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "style cannot be blank");
        }
        if (minTrips < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minTrips cannot be negative");
        }

        return userRepository.findByTravelStyleWithMinimumCompletedTrips(style, minTrips);
    }

    @Transactional
    public User deactivate(Long id) {
        User user = findById(id);
        long activeItinerariesCount = userRepository.countActiveItinerariesForUser(id);
        if (activeItinerariesCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot deactivate user with active itineraries"
            );
        }

        user.setStatus(Status.DEACTIVATED);
        User saved = userRepository.save(user);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", saved.getId());
        notifyObservers("USER_DEACTIVATED", payload);
        return saved;
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
