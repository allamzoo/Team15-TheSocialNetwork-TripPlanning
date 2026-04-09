package com.team15.tripplanning.userservice.controller;

import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.service.UserService;
import com.team15.tripplanning.userservice.service.SavedDestinationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.update(id, user));
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
}

