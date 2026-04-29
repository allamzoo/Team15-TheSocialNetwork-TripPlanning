package com.team15.tripplanning.itineraryservice.repository;

import com.team15.tripplanning.itineraryservice.model.mongo.ItineraryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ItineraryEventRepository extends MongoRepository<ItineraryEvent, String> {

    List<ItineraryEvent> findByItineraryIdOrderByTimestampDesc(Long itineraryId);

    Page<ItineraryEvent> findByItineraryId(Long itineraryId, Pageable pageable);
}
