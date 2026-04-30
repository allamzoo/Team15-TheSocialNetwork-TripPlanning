package com.team15.tripplanning.activityservice.adapter;

import com.team15.tripplanning.activityservice.dto.NearbyActivityDTO;
import com.team15.tripplanning.activityservice.model.cassandra.ActivityLogRow;

public class CassandraRowAdapter {

    public NearbyActivityDTO adapt(ActivityLogRow row) {
        return NearbyActivityDTO.builder()
                .activityId(row.getActivityId())
                .name(row.getName())
                .category(row.getCategory())
                .latitude(row.getLatitude())
                .longitude(row.getLongitude())
                .distanceKm(0.0)
                .build();
    }
}
