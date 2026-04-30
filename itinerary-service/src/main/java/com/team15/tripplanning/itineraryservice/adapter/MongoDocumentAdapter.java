package com.team15.tripplanning.itineraryservice.adapter;

import com.team15.tripplanning.itineraryservice.dto.ItineraryDetailsDTO;
import com.team15.tripplanning.itineraryservice.model.mongo.ItineraryEvent;

public class MongoDocumentAdapter {

    public ItineraryDetailsDTO adapt(ItineraryEvent event) {
        ItineraryDetailsDTO.Builder builder = ItineraryDetailsDTO.builder()
                .itineraryId(event.getItineraryId())
                .userId(event.getUserId())
                .status(event.getAction())
                .metadata(event.getDetails());

        if (event.getDetails() != null) {
            Object destinationId = event.getDetails().get("destinationId");
            if (destinationId instanceof Number n) {
                builder.destinationId(n.longValue());
            }

            Object title = event.getDetails().get("title");
            if (title != null) {
                builder.title(title.toString());
            }

            Object budget = event.getDetails().get("estimatedBudget");
            if (budget instanceof Number n) {
                builder.estimatedBudget(n.doubleValue());
            }
        }

        return builder.build();
    }
}
