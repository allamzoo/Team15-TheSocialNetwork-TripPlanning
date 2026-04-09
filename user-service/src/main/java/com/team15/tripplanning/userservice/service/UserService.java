package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.model.UserRole;
import com.team15.tripplanning.userservice.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void delete(Long id) {
        findById(id);
        userRepository.deleteById(id);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
