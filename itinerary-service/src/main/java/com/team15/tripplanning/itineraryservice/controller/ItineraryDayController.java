package com.team15.tripplanning.itineraryservice.controller;

import com.team15.tripplanning.itineraryservice.model.ItineraryDay;
import com.team15.tripplanning.itineraryservice.service.ItineraryDayService;
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
@RequestMapping("/api/itinerary-days")
public class ItineraryDayController {
    private final ItineraryDayService itineraryDayService;

    public ItineraryDayController(ItineraryDayService itineraryDayService) {
        this.itineraryDayService = itineraryDayService;
    }

    @PostMapping
    public ResponseEntity<ItineraryDay> create(@RequestBody ItineraryDay itineraryDay) {
        return ResponseEntity.ok(itineraryDayService.create(itineraryDay));
    }

    @GetMapping
    public ResponseEntity<List<ItineraryDay>> findAll() {
        return ResponseEntity.ok(itineraryDayService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItineraryDay> findById(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryDayService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItineraryDay> update(@PathVariable Long id, @RequestBody ItineraryDay itineraryDay) {
        return ResponseEntity.ok(itineraryDayService.update(id, itineraryDay));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itineraryDayService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

