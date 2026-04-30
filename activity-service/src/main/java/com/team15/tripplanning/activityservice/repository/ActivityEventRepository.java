package com.team15.tripplanning.activityservice.repository;

import com.team15.tripplanning.activityservice.model.mongo.ActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityEventRepository extends MongoRepository<ActivityEvent, String> {

    List<ActivityEvent> findByActivityIdOrderByTimestampDesc(Long activityId);

    Page<ActivityEvent> findByItineraryId(Long itineraryId, Pageable pageable);
}
