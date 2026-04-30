package com.team15.tripplanning.activityservice.adapter;

import com.team15.tripplanning.activityservice.dto.ActivitySummaryDTO;
import com.team15.tripplanning.activityservice.model.mongo.ActivityEvent;

public class MongoDocumentAdapter {

    public ActivitySummaryDTO adapt(ActivityEvent event) {
        Double cost = parseDouble(event.getDetails(), "cost");

        return ActivitySummaryDTO.builder()
                .itineraryId(event.getItineraryId())
                .totalActivities(1)
                .averageCost(cost)
                .maxCost(cost)
                .firstScheduledTime(event.getTimestamp())
                .lastScheduledTime(event.getTimestamp())
                .build();
    }

    private Double parseDouble(java.util.Map<String, Object> details, String key) {
        Object val = details != null ? details.get(key) : null;
        return val instanceof Number n ? n.doubleValue() : null;
    }
}
