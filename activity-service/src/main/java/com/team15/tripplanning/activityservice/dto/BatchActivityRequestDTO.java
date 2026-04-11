package com.team15.tripplanning.activityservice.dto;

import com.team15.tripplanning.activityservice.model.Activity;
import java.util.List;

public record BatchActivityRequestDTO(
    Long itineraryId,
    List<Activity> activities
) {}
