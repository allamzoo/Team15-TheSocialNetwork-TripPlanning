package com.team15.tripplanning.itineraryservice.adapter;

import com.team15.tripplanning.itineraryservice.dto.TripCostEstimateDTO;
import com.team15.tripplanning.itineraryservice.model.neo4j.TripRelationshipRecord;

public class Neo4jRecordAdapter {

    public TripCostEstimateDTO adapt(TripRelationshipRecord record) {
        double accommodation = orZero(record.getAccommodationCost());
        double transport     = orZero(record.getTransportCost());
        double activities    = orZero(record.getActivitiesCost());
        double multiplier    = record.getSeasonMultiplier() != null ? record.getSeasonMultiplier() : 1.0;
        double total         = (accommodation + transport + activities) * multiplier;

        return TripCostEstimateDTO.builder()
                .estimatedAccommodation(accommodation)
                .estimatedTransport(transport)
                .estimatedActivities(activities)
                .estimatedTotal(total)
                .seasonMultiplier(multiplier)
                .build();
    }

    private double orZero(Double value) {
        return value != null ? value : 0.0;
    }
}
