package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.model.UserRole;
import com.team15.tripplanning.userservice.model.UserStatus;
import com.team15.tripplanning.userservice.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(User user) {
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> search(String name, String email, UserRole role) {
        String normalizedName = normalize(name);
        String normalizedEmail = normalize(email);
        String normalizedRole = role != null ? role.name() : null;
        if (normalizedName == null && normalizedEmail == null && normalizedRole == null) {
            return List.of();
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

    public User update(Long id, User user) {
        User existing = findById(id);
        existing.setName(user.getName());
        existing.setEmail(user.getEmail());
        existing.setPassword(user.getPassword());
        existing.setPhone(user.getPhone());
        existing.setRole(user.getRole());
        existing.setStatus(user.getStatus());
        existing.setPreferences(user.getPreferences());
        return userRepository.save(existing);
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
        return userRepository.save(existing);
    }

    public UserTripSummaryDTO getTripSummary(Long id) {
        findById(id);
        var result = userRepository.getUserTripSummary(id);
        if (result.isEmpty()) {
            return new UserTripSummaryDTO(id, null, 0L, 0L, 0L, 0.0, 0.0);
        }

        Object[] row = unwrapRow(result.get(0));
        Long userId = toLong(row[0]);
        String name = row[1] != null ? row[1].toString() : null;
        Long totalTrips = toLong(row[2]);
        Long completedTrips = toLong(row[3]);
        Long cancelledTrips = toLong(row[4]);
        Double totalSpent = toDouble(row[5]);
        Double averageBudget = toDouble(row[6]);

        return new UserTripSummaryDTO(userId, name, totalTrips, completedTrips, cancelledTrips, totalSpent, averageBudget);
    }

    private Object[] unwrapRow(Object rawRow) {
        if (rawRow instanceof Object[] row && row.length == 1 && row[0] instanceof Object[]) {
            return (Object[]) row[0];
        }
        return (Object[]) rawRow;
    }

    private Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }

    public void delete(Long id) {
        findById(id);
        userRepository.deleteById(id);
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

        user.setStatus(UserStatus.DEACTIVATED);
        return userRepository.save(user);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
