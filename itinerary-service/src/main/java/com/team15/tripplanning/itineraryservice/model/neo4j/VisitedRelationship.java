package com.team15.tripplanning.itineraryservice.model.neo4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;

@RelationshipProperties
public class VisitedRelationship {

    @Id
    @GeneratedValue
    private Long id;

    private Integer visitCount;
    private LocalDateTime lastVisitDate;
    private List<Long> recorded_itinerary_ids = new ArrayList<>();

    public VisitedRelationship() {
    }

    public Long getId() {
        return id;
    }

    public Integer getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(Integer visitCount) {
        this.visitCount = visitCount;
    }

    public LocalDateTime getLastVisitDate() {
        return lastVisitDate;
    }

    public void setLastVisitDate(LocalDateTime lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }

    public List<Long> getRecorded_itinerary_ids() {
        return recorded_itinerary_ids;
    }

    public void setRecorded_itinerary_ids(List<Long> recorded_itinerary_ids) {
        this.recorded_itinerary_ids = recorded_itinerary_ids;
    }
}