package com.team15.tripplanning.bookingservice.feature.settlement;

import com.team15.tripplanning.bookingservice.dto.SettlementProcessRequest;
import com.team15.tripplanning.bookingservice.dto.SettlementResultDTO;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * S5-READ-DB / S5-EVENTS
 *
 * GET  /api/bookings/settlements/{itineraryId}  → SettlementDTO       (read-only query)
 * GET  /api/bookings/settlements                → Page<Settlement>     (ADMIN only)
 * POST /api/bookings/settlement/process         → SettlementResultDTO  (saga transition)
 */
@RestController
@RequestMapping("/api/bookings")
public class SettlementController {

    private final SettlementService    settlementService;
    private final SettlementRepository settlementRepository;

    public SettlementController(SettlementService settlementService,
                                SettlementRepository settlementRepository) {
        this.settlementService    = settlementService;
        this.settlementRepository = settlementRepository;
    }

    /** GET /api/bookings/settlements/{itineraryId} */
    @GetMapping("/settlements/{itineraryId}")
    public ResponseEntity<SettlementDTO> getByItinerary(@PathVariable Long itineraryId) {
        return ResponseEntity.ok(settlementService.getByItineraryId(itineraryId));
    }

    /**
     * GET /api/bookings/settlements
     * Returns a paginated list of all settlements. ADMIN only.
     */
    @GetMapping("/settlements")
    public ResponseEntity<Page<Settlement>> listAll(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        return ResponseEntity.ok(settlementRepository.findAll(pageable));
    }

    /**
     * POST /api/bookings/settlement/process
     * Drives SettlementService.processSettlement() — transitions PENDING → COMPLETED or FAILED.
     */
    @PostMapping("/settlement/process")
    public ResponseEntity<SettlementResultDTO> processSettlement(
            @RequestBody SettlementProcessRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(settlementService.processSettlement(request, httpRequest));
    }
}