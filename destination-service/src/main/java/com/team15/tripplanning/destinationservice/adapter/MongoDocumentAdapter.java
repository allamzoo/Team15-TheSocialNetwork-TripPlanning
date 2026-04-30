package com.team15.tripplanning.destinationservice.adapter;

import com.team15.tripplanning.destinationservice.dto.DestinationDTO;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.model.mongo.DestinationEvent;

public class MongoDocumentAdapter {

    public DestinationDTO adapt(DestinationEvent event) {
        DestinationCategory category = parseCategory(event.getDetails());
        DestinationStatus status = parseStatus(event.getDetails());
        Double rating = parseDouble(event.getDetails(), "rating");
        String name = parseString(event.getDetails(), "name");

        return DestinationDTO.builder()
                .id(event.getDestinationId())
                .name(name)
                .country(parseString(event.getDetails(), "country"))
                .category(category)
                .rating(rating)
                .status(status)
                .build();
    }

    private String parseString(java.util.Map<String, Object> details, String key) {
        Object val = details != null ? details.get(key) : null;
        return val != null ? val.toString() : null;
    }

    private Double parseDouble(java.util.Map<String, Object> details, String key) {
        Object val = details != null ? details.get(key) : null;
        return val instanceof Number n ? n.doubleValue() : null;
    }

    private DestinationCategory parseCategory(java.util.Map<String, Object> details) {
        try {
            String val = parseString(details, "category");
            return val != null ? DestinationCategory.valueOf(val) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private DestinationStatus parseStatus(java.util.Map<String, Object> details) {
        try {
            String val = parseString(details, "status");
            return val != null ? DestinationStatus.valueOf(val) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
