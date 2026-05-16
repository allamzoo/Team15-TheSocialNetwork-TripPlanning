package com.team15.tripplanning.bookingservice.feature.settlement;

import com.team15.tripplanning.bookingservice.repository.SettlementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;

    public SettlementService(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    public SettlementDTO getByItineraryId(Long itineraryId) {
        return settlementRepository.findByItineraryId(itineraryId)
                .map(SettlementDTO::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No settlement for itineraryId=" + itineraryId));
    }
}
