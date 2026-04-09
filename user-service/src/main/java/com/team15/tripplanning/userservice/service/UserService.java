package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.repository.UserRepository;
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

    public List<User> searchByPreference(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Preference key cannot be blank"
            );
        }
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Preference value cannot be blank"
            );
        }
        return userRepository.searchByPreference(key, value);
    }
}
