package com.team15.tripplanning.destinationservice.adapter;

import com.team15.tripplanning.destinationservice.dto.DestinationDTO;
import com.team15.tripplanning.destinationservice.model.DestinationCategory;
import com.team15.tripplanning.destinationservice.model.DestinationStatus;
import com.team15.tripplanning.destinationservice.model.elasticsearch.DestinationSearchDocument;

public class ElasticsearchHitAdapter {

    public DestinationDTO adapt(DestinationSearchDocument doc) {
        return DestinationDTO.builder()
                .id(Long.parseLong(doc.getId()))
                .name(doc.getName())
                .country(doc.getCountry())
                .category(parseCategory(doc.getCategory()))
                .rating(doc.getRating())
                .status(parseStatus(doc.getStatus()))
                .build();
    }

    private DestinationCategory parseCategory(String value) {
        try {
            return value != null ? DestinationCategory.valueOf(value) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private DestinationStatus parseStatus(String value) {
        try {
            return value != null ? DestinationStatus.valueOf(value) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
