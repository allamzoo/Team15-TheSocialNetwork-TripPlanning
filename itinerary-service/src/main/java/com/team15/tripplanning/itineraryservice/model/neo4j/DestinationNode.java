package com.team15.tripplanning.itineraryservice.model.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("DestinationNode")
public class DestinationNode {

    @Id
    @GeneratedValue
    private Long id;

    private Long destinationId;
    private String name;
    private String country;
    private String category;

    public DestinationNode() {
    }

    public DestinationNode(Long destinationId, String name, String country, String category) {
        this.destinationId = destinationId;
        this.name = name;
        this.country = country;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}