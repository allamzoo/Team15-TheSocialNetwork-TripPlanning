package com.team15.tripplanning.bookingservice.feature.settlement;

import com.team15.tripplanning.bookingservice.model.Settlement;
import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bookings/settlements")
public class SettlementController {

    private final SettlementRepository settlementRepository;

    public SettlementController(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    /**
     * GET /api/bookings/settlements/{itineraryId}
     * Returns the settlement for a given itinerary.
     */
    @GetMapping("/{itineraryId}")
    public ResponseEntity<SettlementDTO> getByItineraryId(@PathVariable Long itineraryId) {
        Settlement settlement = settlementRepository.findByItineraryId(itineraryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No settlement found for itineraryId=" + itineraryId));
        return ResponseEntity.ok(toDTO(settlement));
    }

    /**
     * GET /api/bookings/settlements
     * Returns a paginated list of all settlements. ADMIN only.
     */
    @GetMapping
    public ResponseEntity<Page<SettlementDTO>> listAll(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {

        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        Page<SettlementDTO> page = settlementRepository.findAll(pageable).map(this::toDTO);
        return ResponseEntity.ok(page);
    }

    private SettlementDTO toDTO(Settlement s) {
        return new SettlementDTO(
                s.getId(),
                s.getItineraryId(),
                s.getAmount(),
                s.getStatus().name(),
                s.getCreatedAt(),
                s.getSettledAt()
        );
    }
}
