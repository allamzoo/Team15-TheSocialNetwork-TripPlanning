package com.team15.tripplanning.destinationservice.controller;

import com.team15.tripplanning.destinationservice.dto.DestinationDashboardDTO;
import com.team15.tripplanning.destinationservice.service.DestinationDashboardService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/destinations")
public class DestinationDashboardController {

    @Autowired
    private DestinationDashboardService dashboardService;

    /**
     * S2-F12: GET /api/destinations/{id}/dashboard
     * Auth: Required (USER) — enforced by Spring Security config
     */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<DestinationDashboardDTO> getDashboard(@PathVariable Long id) {
        DestinationDashboardDTO dto = dashboardService.getDashboard(id);
        return ResponseEntity.ok(dto);
    }
}