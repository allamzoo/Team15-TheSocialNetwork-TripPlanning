package com.team15.tripplanning.destinationservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "destinations" , createIndex = false)
public class DestinationSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String country;

    @Field(type = FieldType.Text)
    private String category;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Text)
    private String highlights;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Integer)
    private Integer totalRatings;

    @Field(type = FieldType.Keyword)
    private String status;

    public DestinationSearchDocument() {
    }

    public DestinationSearchDocument(Long id, String name, String country, String category,
                                     String description, String highlights, Double rating,
                                     Integer totalRatings, String status) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.category = category;
        this.description = description;
        this.highlights = highlights;
        this.rating = rating;
        this.totalRatings = totalRatings;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHighlights() {
        return highlights;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}


