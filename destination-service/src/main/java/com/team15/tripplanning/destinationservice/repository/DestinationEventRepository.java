package com.team15.tripplanning.destinationservice.repository;

import com.team15.tripplanning.destinationservice.model.mongo.DestinationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DestinationEventRepository extends MongoRepository<DestinationEvent, String> {

    List<DestinationEvent> findByDestinationIdOrderByTimestampDesc(Long destinationId);

    Page<DestinationEvent> findByDestinationId(Long destinationId, Pageable pageable);
}
