package com.team15.tripplanning.userservice.controller;

import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import com.team15.tripplanning.userservice.dto.TopTravelerDTO;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.model.UserRole;
import com.team15.tripplanning.userservice.service.UserService;
import com.team15.tripplanning.userservice.service.SavedDestinationService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final SavedDestinationService savedDestinationService;

    public UserController(UserService userService, SavedDestinationService savedDestinationService) {
        this.userService = userService;
        this.savedDestinationService = savedDestinationService;
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        return ResponseEntity.ok(userService.create(user));
    }

    @GetMapping
    public ResponseEntity<List<User>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role
    ) {
        return ResponseEntity.ok(userService.search(name, email, role != null ? Enum.valueOf(com.team15.tripplanning.userservice.model.UserRole.class, role) : null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/{id}/trip-summary")
    public ResponseEntity<UserTripSummaryDTO> getTripSummary(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getTripSummary(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.update(id, user));
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<User> updatePreferences(
            @PathVariable Long id,
            @RequestBody Map<String, Object> newPreferences
    ) {
        return ResponseEntity.ok(userService.mergePreferences(id, newPreferences));
    }

    @PutMapping("/{userId}/destinations/{destinationId}/default")
    public ResponseEntity<User> setDefaultSavedDestination(
            @PathVariable Long userId,
            @PathVariable Long destinationId
    ) {
        return ResponseEntity.ok(savedDestinationService.setDefaultSavedDestination(userId, destinationId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports/top-travelers")
    public ResponseEntity<List<TopTravelerDTO>> getTopTravelersBySpending(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam int limit) {
        return ResponseEntity.ok(userService.getTopTravelersBySpending(startDate, endDate, limit));
    }

    @GetMapping("/preferences/search")
    public ResponseEntity<List<User>> searchByPreference(
            @RequestParam String key,
            @RequestParam String value) {
        return ResponseEntity.ok(userService.searchByPreference(key, value));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<User> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }
}

