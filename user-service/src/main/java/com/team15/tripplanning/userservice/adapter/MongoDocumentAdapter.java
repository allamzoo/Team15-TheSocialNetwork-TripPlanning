package com.team15.tripplanning.userservice.adapter;

import com.team15.tripplanning.userservice.document.AuthEvent;
import com.team15.tripplanning.userservice.dto.ActivityFeedDTO;

public class MongoDocumentAdapter {

    public ActivityFeedDTO adapt(AuthEvent event) {
        return ActivityFeedDTO.builder()
                .action(event.getAction())
                .timestamp(event.getTimestamp())
                .userId(event.getUserId())
                .details(event.getDetails())
                .build();
    }
}
