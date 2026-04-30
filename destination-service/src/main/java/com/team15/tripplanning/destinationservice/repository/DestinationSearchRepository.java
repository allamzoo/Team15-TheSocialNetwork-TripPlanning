package com.team15.tripplanning.destinationservice.repository;

import com.team15.tripplanning.destinationservice.model.DestinationSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinationSearchRepository extends ElasticsearchRepository<DestinationSearchDocument, Long> {
}

