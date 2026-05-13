package com.team15.tripplanning.userservice.controller;

import com.team15.tripplanning.userservice.dto.TopTravelerDTO;
import com.team15.tripplanning.userservice.dto.UserProfileDTO;
import com.team15.tripplanning.userservice.dto.UserTripSummaryDTO;
import com.team15.tripplanning.userservice.model.SavedDestination;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.model.Role;
import com.team15.tripplanning.userservice.security.JwtService;
import com.team15.tripplanning.userservice.service.SavedDestinationService;
import com.team15.tripplanning.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final SavedDestinationService savedDestinationService;
    private final JwtService jwtService;

    public UserController(UserService userService, SavedDestinationService savedDestinationService,
                          JwtService jwtService) {
        this.userService = userService;
        this.savedDestinationService = savedDestinationService;
        this.jwtService = jwtService;
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
        return ResponseEntity.ok(userService.search(name, email, parseRole(role)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id, HttpServletRequest request) {
        checkOwnership(id, request);
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileDTO> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }

    @GetMapping("/{id}/trip-summary")
    public ResponseEntity<UserTripSummaryDTO> getTripSummary(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getTripSummary(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user,
                                       HttpServletRequest request) {
        checkOwnership(id, request);
        return ResponseEntity.ok(userService.update(id, user));
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<User> updatePreferences(
            @PathVariable Long id,
            @RequestBody Map<String, Object> newPreferences
    ) {
        return ResponseEntity.ok(userService.mergePreferences(id, newPreferences));
    }

    @PostMapping("/{userId}/saved-destinations")
    public ResponseEntity<SavedDestination> createSavedDestination(
            @PathVariable Long userId,
            @RequestBody SavedDestination savedDestination
    ) {
        return ResponseEntity.ok(savedDestinationService.create(userId, savedDestination));
    }

    @GetMapping("/{userId}/saved-destinations")
    public ResponseEntity<List<SavedDestination>> findSavedDestinationsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(savedDestinationService.findByUserId(userId));
    }

    @GetMapping("/{userId}/saved-destinations/{destinationId}")
    public ResponseEntity<SavedDestination> findSavedDestinationById(
            @PathVariable Long userId,
            @PathVariable Long destinationId
    ) {
        return ResponseEntity.ok(savedDestinationService.findByIdForUser(userId, destinationId));
    }

    @DeleteMapping("/{userId}/saved-destinations/{destinationId}")
    public ResponseEntity<Void> deleteSavedDestinationById(
            @PathVariable Long userId,
            @PathVariable Long destinationId
    ) {
        savedDestinationService.deleteForUser(userId, destinationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/saved-destinations/{destinationId}/default")
    public ResponseEntity<User> setDefaultSavedDestination(
            @PathVariable Long userId,
            @PathVariable Long destinationId
    ) {
        return ResponseEntity.ok(savedDestinationService.setDefaultSavedDestination(userId, destinationId));
    }

    @PutMapping("/{userId}/destinations/{destinationId}/default")
    public ResponseEntity<User> setDefaultSavedDestinationLegacyPath(
            @PathVariable Long userId,
            @PathVariable Long destinationId
    ) {
        return ResponseEntity.ok(savedDestinationService.setDefaultSavedDestination(userId, destinationId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        checkOwnership(id, request);
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

    @GetMapping("/preferences/travel-style")
    public ResponseEntity<List<User>> findByTravelStyleWithMinimumTrips(
            @RequestParam String style,
            @RequestParam(defaultValue = "0") int minTrips
    ) {
        return ResponseEntity.ok(userService.findByTravelStyleWithMinimumTrips(style, minTrips));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<User> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String roleStr = body.get("role");
        if (roleStr == null || roleStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role field is required");
        }
        try {
            User update = new User();
            update.setRole(Role.valueOf(roleStr.trim().toUpperCase()));
            return ResponseEntity.ok(userService.update(id, update));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleStr);
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<User> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<Map<String, Object>> getActivityFeed(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be > 0");
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        Long callerId = jwtService.extractUserId(token);
        String callerRole = jwtService.extractRole(token);
        if (!id.equals(callerId) && !"ADMIN".equals(callerRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return ResponseEntity.ok(userService.getActivityFeed(id, page, size));
    }

    // ── Ownership guard ────────────────────────────────────────────────────────
    private void checkOwnership(Long resourceId, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        try {
            Long callerId = jwtService.extractUserId(token);
            String callerRole = jwtService.extractRole(token);
            if (!resourceId.equals(callerId) && !"ADMIN".equals(callerRole)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
    }
}
