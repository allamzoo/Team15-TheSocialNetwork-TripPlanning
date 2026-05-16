package com.team15.tripplanning.bookingservice.feature.settlement;

import com.team15.tripplanning.bookingservice.dto.SettlementProcessRequest;
import com.team15.tripplanning.bookingservice.dto.SettlementResultDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/process")
    public ResponseEntity<SettlementResultDTO> processSettlement(
            @RequestBody SettlementProcessRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(settlementService.processSettlement(request, httpRequest));
    }
}

