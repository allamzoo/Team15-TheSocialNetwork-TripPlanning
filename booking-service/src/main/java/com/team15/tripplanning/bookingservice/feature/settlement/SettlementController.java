package com.team15.tripplanning.bookingservice.feature.settlement;

import com.team15.tripplanning.bookingservice.saga.SettlementSaga;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;
import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import java.math.BigDecimal;
import java.util.Map;

/**
 * S5-READ-DB / S5-EVENTS
 *
 * GET  /api/bookings/settlements/{itineraryId}  → SettlementDTO  (read-only query)
 * POST /api/bookings/settlement/process          → SettlementDTO  (saga transition)
 */
@RestController
@RequestMapping("/api/bookings")
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementSaga    settlementSaga;

    public SettlementController(SettlementService settlementService,
                                SettlementSaga settlementSaga) {
        this.settlementService = settlementService;
        this.settlementSaga    = settlementSaga;
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
     * Body: { "itineraryId": 20, "userId": 1, "amount": 2000 }
     *
     * Drives SettlementSaga.process() — transitions SETTLEMENT_PENDING → SETTLED or PAYMENT_FAILED.
     */
    @PostMapping("/settlement/process")
    public ResponseEntity<SettlementDTO> process(@RequestBody Map<String, Object> body) {
        Long itineraryId = Long.valueOf(body.get("itineraryId").toString());
        Long userId      = Long.valueOf(body.get("userId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        var settlement = settlementSaga.process(itineraryId, userId, amount);
        return ResponseEntity.ok(SettlementDTO.from(settlement));
    }
}
