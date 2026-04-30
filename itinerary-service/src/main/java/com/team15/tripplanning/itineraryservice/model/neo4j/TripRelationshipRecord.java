package com.team15.tripplanning.itineraryservice.model.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("TripRoute")
public class TripRelationshipRecord {

    @Id
    @GeneratedValue
    private Long id;

    private String startDestination;
    private String endDestination;
    private Double transportCost;
    private Double accommodationCost;
    private Double activitiesCost;
    private Double seasonMultiplier;

    public TripRelationshipRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStartDestination() { return startDestination; }
    public void setStartDestination(String startDestination) { this.startDestination = startDestination; }

    public String getEndDestination() { return endDestination; }
    public void setEndDestination(String endDestination) { this.endDestination = endDestination; }

    public Double getTransportCost() { return transportCost; }
    public void setTransportCost(Double transportCost) { this.transportCost = transportCost; }

    public Double getAccommodationCost() { return accommodationCost; }
    public void setAccommodationCost(Double accommodationCost) { this.accommodationCost = accommodationCost; }

    public Double getActivitiesCost() { return activitiesCost; }
    public void setActivitiesCost(Double activitiesCost) { this.activitiesCost = activitiesCost; }

    public Double getSeasonMultiplier() { return seasonMultiplier; }
    public void setSeasonMultiplier(Double seasonMultiplier) { this.seasonMultiplier = seasonMultiplier; }
}
