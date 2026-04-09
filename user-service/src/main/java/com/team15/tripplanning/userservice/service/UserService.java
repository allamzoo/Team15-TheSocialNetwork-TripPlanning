package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.userservice.dto.TopTravelerDTO;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public void delete(Long id) {
        findById(id);
        userRepository.deleteById(id);
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

    private TopTravelerDTO mapTopTravelerRow(Object[] row) {
        Long userId = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        String name = row[1] != null ? row[1].toString() : null;
        Double totalSpent = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        Long tripCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;

        return new TopTravelerDTO(userId, name, totalSpent, tripCount);
    }
}
