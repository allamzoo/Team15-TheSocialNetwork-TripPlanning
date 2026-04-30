package com.team15.tripplanning.itineraryservice.adapter;

import com.team15.tripplanning.itineraryservice.dto.ItineraryDetailsDTO;
import com.team15.tripplanning.itineraryservice.model.mongo.ItineraryEvent;

public class MongoDocumentAdapter {

    public ItineraryDetailsDTO adapt(ItineraryEvent event) {
        ItineraryDetailsDTO dto = new ItineraryDetailsDTO();
        dto.setItineraryId(event.getItineraryId());
        dto.setUserId(event.getUserId());
        dto.setStatus(event.getAction());
        dto.setMetadata(event.getDetails());

        if (event.getDetails() != null) {
            Object destinationId = event.getDetails().get("destinationId");
            if (destinationId instanceof Number n) {
                dto.setDestinationId(n.longValue());
            }

            Object title = event.getDetails().get("title");
            if (title != null) {
                dto.setTitle(title.toString());
            }

            Object budget = event.getDetails().get("estimatedBudget");
            if (budget instanceof Number n) {
                dto.setEstimatedBudget(n.doubleValue());
            }
        }

        return dto;
    }
}
