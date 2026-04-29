package com.team15.tripplanning.userservice.controller;

import com.team15.tripplanning.userservice.entity.SavedDestination;
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
@RequestMapping("/api/saved-destinations")
public class SavedDestinationController {
    private final SavedDestinationService savedDestinationService;

    public SavedDestinationController(SavedDestinationService savedDestinationService) {
        this.savedDestinationService = savedDestinationService;
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<SavedDestination> create(
            @PathVariable Long userId,
            @RequestBody SavedDestination savedDestination
    ) {
        return ResponseEntity.ok(savedDestinationService.create(userId, savedDestination));
    }

    @GetMapping
    public ResponseEntity<List<SavedDestination>> findAll() {
        return ResponseEntity.ok(savedDestinationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedDestination> findById(@PathVariable Long id) {
        return ResponseEntity.ok(savedDestinationService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavedDestination> update(@PathVariable Long id, @RequestBody SavedDestination savedDestination) {
        return ResponseEntity.ok(savedDestinationService.update(id, savedDestination));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        savedDestinationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
